package com.worksure.web;

import com.worksure.web.request.SendChatMessageRequest;

import com.worksure.db.Db;
import com.worksure.security.AuthUser;
import com.worksure.security.SecurityUtils;
import com.worksure.socket.RealtimeService;
import com.worksure.util.RowMaps;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final Db db;
    private final RealtimeService realtime;

    public ChatController(Db db, RealtimeService realtime) {
        this.db = db;
        this.realtime = realtime;
    }

    @GetMapping("/{bookingId}")
    public Map<String, Object> list(@PathVariable long bookingId) {
        AuthUser auth = SecurityUtils.currentUser();
        Map<String, Object> booking = loadBooking(bookingId);
        boolean isCustomer = RowMaps.asLong(booking.get("customer_id")) == auth.id();
        boolean isWorker = RowMaps.asLong(booking.get("worker_user_id")) == auth.id();
        if (!isCustomer && !isWorker && !"admin".equals(auth.role())) {
            throw new ApiException(403, "Forbidden");
        }
        return ApiResponses.ok("messages", db.query(
                """
                SELECT m.*, u.full_name AS sender_name, u.avatar_url AS sender_avatar
                FROM messages m JOIN users u ON u.id = m.sender_id
                WHERE m.booking_id = ? ORDER BY m.id ASC
                """,
                bookingId
        ));
    }

    @PostMapping("/{bookingId}")
    public ResponseEntity<Map<String, Object>> send(@PathVariable long bookingId, @RequestBody SendChatMessageRequest body) {
        AuthUser auth = SecurityUtils.currentUser();
        String content = String.valueOf(body.getContent()).trim();
        if (content.isBlank()) {
            throw new ApiException(400, "content is required");
        }
        Map<String, Object> booking = loadBooking(bookingId);
        boolean isCustomer = RowMaps.asLong(booking.get("customer_id")) == auth.id();
        boolean isWorker = RowMaps.asLong(booking.get("worker_user_id")) == auth.id();
        if (!isCustomer && !isWorker) {
            throw new ApiException(403, "Forbidden");
        }
        long id = db.insert("INSERT INTO messages (booking_id, sender_id, content) VALUES (?, ?, ?)",
                bookingId, auth.id(), content);
        Map<String, Object> msg = db.queryOne(
                "SELECT m.*, u.full_name AS sender_name FROM messages m JOIN users u ON u.id = m.sender_id WHERE m.id = ?",
                id
        );
        long recipient = isCustomer ? RowMaps.asLong(booking.get("worker_user_id")) : RowMaps.asLong(booking.get("customer_id"));
        realtime.emitChat(String.valueOf(bookingId), msg, recipient);
        return ResponseEntity.status(201).body(ApiResponses.ok("message", msg));
    }

    private Map<String, Object> loadBooking(long bookingId) {
        Map<String, Object> booking = db.queryOne(
                "SELECT b.*, w.user_id AS worker_user_id FROM bookings b JOIN workers w ON w.id = b.worker_id WHERE b.id = ?",
                bookingId
        );
        if (booking == null) {
            throw new ApiException(404, "Booking not found");
        }
        return booking;
    }
}
