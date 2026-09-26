package com.worksure.web;

import com.worksure.web.request.CreateBookingRequest;
import com.worksure.web.request.UpdateBookingStatusRequest;
import com.worksure.web.request.UpdateBookingTrackingRequest;

import com.worksure.db.Db;
import com.worksure.security.AuthUser;
import com.worksure.security.SecurityUtils;
import com.worksure.socket.RealtimeService;
import com.worksure.util.DateTimes;
import com.worksure.util.RowMaps;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final Db db;
    private final RealtimeService realtime;

    public BookingController(Db db, RealtimeService realtime) {
        this.db = db;
        this.realtime = realtime;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateBookingRequest body) {
        AuthUser auth = SecurityUtils.currentUser();
        SecurityUtils.requireRole(auth, "customer");
        Long serviceId = body.getServiceId();
        String scheduled = String.valueOf(body.getScheduledAt());
        String address = String.valueOf(body.getAddress()).trim();
        if (serviceId == null || scheduled.isBlank() || address.isBlank()) {
            throw new ApiException(400, "service_id, scheduled_at and address are required");
        }
        Map<String, Object> service = db.queryOne(
                """
                SELECT s.*, w.id AS worker_pk, w.user_id AS worker_owner_id FROM services s
                JOIN workers w ON w.id = s.worker_id WHERE s.id = ? AND s.is_active = 1
                """,
                serviceId
        );
        if (service == null) {
            throw new ApiException(404, "Service not found");
        }
        if (RowMaps.asLong(service.get("worker_owner_id")) == auth.id()) {
            throw new ApiException(400, "Cannot book your own service");
        }
        String scheduledMysql = DateTimes.toMysqlDatetime(scheduled);
        long id = db.insert(
                """
                INSERT INTO bookings (customer_id, worker_id, service_id, scheduled_at, address, notes, total_price, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'pending')
                """,
                auth.id(), service.get("worker_pk"), serviceId, scheduledMysql,
                address, emptyToNull(String.valueOf(body.getNotes())), service.get("base_price")
        );
        Map<String, Object> booking = db.queryOne("SELECT * FROM bookings WHERE id = ?", id);
        realtime.recordBookingStatus(id, "pending", auth.id(), "Booking created");
        realtime.notifyUser(RowMaps.asLong(service.get("worker_owner_id")), "booking", "New booking request",
                "A customer booked " + service.get("title"), Map.of("booking_id", id));
        realtime.notifyUser(auth.id(), "booking", "Booking placed", "Your request was sent to the worker.",
                Map.of("booking_id", id));
        realtime.emitBookingUpdate(booking);
        return ResponseEntity.status(201).body(ApiResponses.ok("booking", booking));
    }

    @GetMapping
    public Map<String, Object> listMine() {
        AuthUser auth = SecurityUtils.currentUser();
        List<Map<String, Object>> bookings;
        if ("customer".equals(auth.role())) {
            bookings = db.query(
                    """
                    SELECT b.*, s.title AS service_title, u.full_name AS worker_name
                    FROM bookings b
                    JOIN services s ON s.id = b.service_id
                    JOIN workers w ON w.id = b.worker_id
                    JOIN users u ON u.id = w.user_id
                    WHERE b.customer_id = ? ORDER BY b.id DESC
                    """,
                    auth.id()
            );
        } else if ("worker".equals(auth.role())) {
            Map<String, Object> w = db.queryOne("SELECT id FROM workers WHERE user_id = ?", auth.id());
            if (w == null) {
                return ApiResponses.ok("bookings", List.of());
            }
            bookings = db.query(
                    """
                    SELECT b.*, s.title AS service_title, u.full_name AS customer_name
                    FROM bookings b
                    JOIN services s ON s.id = b.service_id
                    JOIN users u ON u.id = b.customer_id
                    WHERE b.worker_id = ? ORDER BY b.id DESC
                    """,
                    w.get("id")
            );
        } else {
            bookings = db.query(
                    "SELECT b.*, s.title AS service_title FROM bookings b JOIN services s ON s.id = b.service_id ORDER BY b.id DESC LIMIT 100"
            );
        }
        return ApiResponses.ok("bookings", bookings);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable long id) {
        AuthUser auth = SecurityUtils.currentUser();
        Map<String, Object> booking = db.queryOne(
                """
                SELECT b.*, s.title AS service_title, s.description AS service_description,
                       w.user_id AS worker_user_id, cust.full_name AS customer_name, work.full_name AS worker_name
                FROM bookings b
                JOIN services s ON s.id = b.service_id
                JOIN workers w ON w.id = b.worker_id
                JOIN users cust ON cust.id = b.customer_id
                JOIN users work ON work.id = w.user_id
                WHERE b.id = ?
                """,
                id
        );
        if (booking == null) {
            throw new ApiException(404, "Not found");
        }
        boolean isCustomer = RowMaps.asLong(booking.get("customer_id")) == auth.id();
        boolean isWorker = RowMaps.asLong(booking.get("worker_user_id")) == auth.id();
        if (!isCustomer && !isWorker && !"admin".equals(auth.role())) {
            throw new ApiException(403, "Forbidden");
        }
        Map<String, Object> res = ApiResponses.ok("booking", booking);
        res.put("status_history", db.query(
                """
                SELECT h.*, u.full_name AS changed_by_name FROM booking_status_history h
                LEFT JOIN users u ON u.id = h.changed_by WHERE h.booking_id = ? ORDER BY h.id ASC
                """,
                id
        ));
        return res;
    }

    @PatchMapping("/{id}/status")
    public Map<String, Object> status(@PathVariable long id, @RequestBody UpdateBookingStatusRequest body) {
        AuthUser auth = SecurityUtils.currentUser();
        String status = String.valueOf(body.getStatus());
        if (!List.of("accepted", "rejected", "in_progress", "completed", "cancelled").contains(status)) {
            throw new ApiException(400, "Invalid status");
        }
        Map<String, Object> booking = db.queryOne("SELECT * FROM bookings WHERE id = ?", id);
        if (booking == null) {
            throw new ApiException(404, "Not found");
        }
        Map<String, Object> worker = db.queryOne("SELECT id, user_id FROM workers WHERE id = ?", booking.get("worker_id"));
        boolean isWorker = worker != null && RowMaps.asLong(worker.get("user_id")) == auth.id();
        boolean isCustomer = RowMaps.asLong(booking.get("customer_id")) == auth.id();
        boolean workerAction = List.of("accepted", "rejected", "in_progress", "completed").contains(status);
        if (workerAction) {
            if (!"worker".equals(auth.role()) || !isWorker) {
                throw new ApiException(403, "Only the assigned worker can perform this job action");
            }
            String requiredStatus = switch (status) {
                case "accepted", "rejected" -> "pending";
                case "in_progress" -> "accepted";
                default -> "in_progress";
            };
            if (!requiredStatus.equals(String.valueOf(booking.get("status")))) {
                throw new ApiException(400, "Cannot change booking from " + booking.get("status") + " to " + status);
            }
        }
        if ("cancelled".equals(status) && !isCustomer && !isWorker && !"admin".equals(auth.role())) {
            throw new ApiException(403, "Forbidden");
        }
        if (workerAction) {
            int changed = db.run("UPDATE bookings SET status = ? WHERE id = ? AND status = ?",
                    status, id, booking.get("status"));
            if (changed == 0) {
                throw new ApiException(409, "Booking status changed. Refresh and try again.");
            }
        } else {
            db.run("UPDATE bookings SET status = ? WHERE id = ?", status, id);
        }
        realtime.recordBookingStatus(id, status, auth.id(), null);
        Map<String, Object> updated = db.queryOne("SELECT * FROM bookings WHERE id = ?", id);
        realtime.notifyUser(RowMaps.asLong(booking.get("customer_id")), "booking", "Booking update",
                "Status is now " + status, Map.of("booking_id", id));
        if (worker != null && worker.get("user_id") != null) {
            realtime.notifyUser(RowMaps.asLong(worker.get("user_id")), "booking", "Booking update",
                    "Status is now " + status, Map.of("booking_id", id));
        }
        realtime.emitBookingUpdate(updated);
        return ApiResponses.ok("booking", updated);
    }

    @PatchMapping("/{id}/tracking")
    public Map<String, Object> tracking(@PathVariable long id, @RequestBody UpdateBookingTrackingRequest body) {
        AuthUser auth = SecurityUtils.currentUser();
        Map<String, Object> booking = db.queryOne("SELECT * FROM bookings WHERE id = ?", id);
        if (booking == null) {
            throw new ApiException(404, "Not found");
        }
        Map<String, Object> worker = db.queryOne("SELECT user_id FROM workers WHERE id = ?", booking.get("worker_id"));
        if ((worker == null || RowMaps.asLong(worker.get("user_id")) != auth.id()) && !"admin".equals(auth.role())) {
            throw new ApiException(403, "Forbidden");
        }
        String note = body.getTrackingNote();
        db.run("UPDATE bookings SET tracking_note = ? WHERE id = ?", note, id);
        Map<String, Object> updated = db.queryOne("SELECT * FROM bookings WHERE id = ?", id);
        realtime.notifyUser(RowMaps.asLong(booking.get("customer_id")), "booking", "Order tracking",
                note == null ? "Updated" : String.valueOf(note), Map.of("booking_id", id));
        realtime.emitBookingUpdate(updated);
        return ApiResponses.ok("booking", updated);
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() || "null".equals(s) ? null : s;
    }
}
