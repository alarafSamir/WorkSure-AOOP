package com.worksure.web;

import com.worksure.web.request.CreateReviewRequest;

import com.worksure.db.Db;
import com.worksure.security.AuthUser;
import com.worksure.security.SecurityUtils;
import com.worksure.util.RowMaps;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final Db db;

    public ReviewController(Db db) {
        this.db = db;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateReviewRequest body) {
        AuthUser auth = SecurityUtils.currentUser();
        SecurityUtils.requireRole(auth, "customer", "admin");
        Long bookingId = body.getBookingId();
        int rating;
        try {
            rating = Integer.parseInt(String.valueOf(body.getRating()));
        } catch (Exception e) {
            throw new ApiException(400, "rating is required");
        }
        if (rating < 1 || rating > 5) {
            throw new ApiException(400, "rating must be 1-5");
        }
        Map<String, Object> booking = db.queryOne(
                "SELECT b.*, w.id AS worker_pk FROM bookings b JOIN workers w ON w.id = b.worker_id WHERE b.id = ?",
                bookingId
        );
        if (booking == null) {
            throw new ApiException(404, "Booking not found");
        }
        if (RowMaps.asLong(booking.get("customer_id")) != auth.id()) {
            throw new ApiException(403, "Only customer can review");
        }
        if (!"completed".equals(String.valueOf(booking.get("status")))) {
            throw new ApiException(400, "Complete the booking before reviewing");
        }
        if (db.queryOne("SELECT id FROM reviews WHERE booking_id = ?", bookingId) != null) {
            throw new ApiException(409, "Already reviewed");
        }
        String comment = body.getComment();
        db.run("INSERT INTO reviews (booking_id, reviewer_id, worker_id, rating, comment) VALUES (?, ?, ?, ?, ?)",
                bookingId, auth.id(), booking.get("worker_pk"), rating, comment);
        Map<String, Object> agg = db.queryOne(
                "SELECT AVG(rating) AS avg_rating, COUNT(*) AS cnt FROM reviews WHERE worker_id = ?",
                booking.get("worker_pk")
        );
        db.run("UPDATE workers SET rating_avg = ?, rating_count = ? WHERE id = ?",
                String.format("%.2f", Double.parseDouble(String.valueOf(agg.get("avg_rating")))),
                agg.get("cnt"),
                booking.get("worker_pk"));
        Map<String, Object> review = db.queryOne("SELECT * FROM reviews WHERE booking_id = ?", bookingId);
        return ResponseEntity.status(201).body(ApiResponses.ok("review", review));
    }

    @GetMapping("/given")
    public Map<String, Object> given() {
        AuthUser auth = SecurityUtils.currentUser();
        SecurityUtils.requireRole(auth, "customer", "admin");
        return ApiResponses.ok("reviews", db.query(
                """
                SELECT r.*, s.title AS service_title FROM reviews r
                JOIN bookings b ON b.id = r.booking_id
                JOIN services s ON s.id = b.service_id
                WHERE r.reviewer_id = ? ORDER BY r.id DESC
                """,
                auth.id()
        ));
    }

    @GetMapping("/worker")
    public Map<String, Object> forWorker() {
        AuthUser auth = SecurityUtils.currentUser();
        SecurityUtils.requireRole(auth, "worker");
        Map<String, Object> worker = db.queryOne("SELECT id FROM workers WHERE user_id = ?", auth.id());
        if (worker == null) {
            throw new ApiException(404, "Worker not found");
        }
        return ApiResponses.ok("reviews", db.query(
                """
                SELECT r.*, u.full_name AS reviewer_name FROM reviews r
                JOIN users u ON u.id = r.reviewer_id WHERE r.worker_id = ? ORDER BY r.id DESC
                """,
                worker.get("id")
        ));
    }
}
