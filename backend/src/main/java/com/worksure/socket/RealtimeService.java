package com.worksure.socket;

import com.corundumstudio.socketio.SocketIOServer;
import com.worksure.db.Db;
import com.worksure.util.RowMaps;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RealtimeService {
    private final SocketIOServer io;
    private final Db db;

    public RealtimeService(SocketIOServer io, Db db) {
        this.io = io;
        this.db = db;
    }

    public Map<String, Object> notifyUser(long userId, String type, String title, String body, Object data) {
        db.run(
                "INSERT INTO notifications (user_id, type, title, body, data) VALUES (?, ?, ?, ?, ?)",
                userId, type, title, body, data != null ? RowMaps.json(data) : null
        );
        Map<String, Object> n = db.queryOne(
                "SELECT id, user_id, type, title, body, data, is_read, created_at FROM notifications WHERE user_id = ? ORDER BY id DESC LIMIT 1",
                userId
        );
        io.getRoomOperations("user:" + userId).sendEvent("notification", n);
        return n;
    }

    public void emitBookingUpdate(Map<String, Object> booking) {
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("booking", booking);
        long customerId = RowMaps.asLong(booking.get("customer_id"));
        io.getRoomOperations("user:" + customerId).sendEvent("booking:update", payload);
        Map<String, Object> worker = db.queryOne("SELECT user_id FROM workers WHERE id = ?", booking.get("worker_id"));
        if (worker != null && worker.get("user_id") != null) {
            io.getRoomOperations("user:" + RowMaps.asLong(worker.get("user_id"))).sendEvent("booking:update", payload);
        }
    }

    public void emitChat(String bookingId, Map<String, Object> msg, Long recipientUserId) {
        io.getRoomOperations("booking:" + bookingId).sendEvent("chat:message", msg);
        if (recipientUserId != null) {
            io.getRoomOperations("user:" + recipientUserId).sendEvent("chat:message", msg);
        }
    }

    public void recordBookingStatus(long bookingId, String status, Long changedBy, String note) {
        db.run(
                "INSERT INTO booking_status_history (booking_id, status, changed_by, note) VALUES (?, ?, ?, ?)",
                bookingId, status, changedBy, note
        );
    }
}
