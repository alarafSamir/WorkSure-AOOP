package com.worksure.web;

import com.worksure.web.request.ReviewDocumentRequest;
import com.worksure.web.request.UpdateUserBanRequest;
import com.worksure.web.request.SuspendUserRequest;
import com.worksure.web.request.UpdateComplaintRequest;

import com.worksure.db.Db;
import com.worksure.storage.VerificationDocuments;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import com.worksure.security.AuthUser;
import com.worksure.security.SecurityUtils;
import com.worksure.util.RowMaps;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final Db db;
    private final VerificationDocuments documents;

    public AdminController(Db db, VerificationDocuments documents) {
        this.db = db;
        this.documents = documents;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("users", num(db.queryOne("SELECT COUNT(*) AS c FROM users").get("c")));
        stats.put("workers", num(db.queryOne("SELECT COUNT(*) AS c FROM workers").get("c")));
        stats.put("bookings", num(db.queryOne("SELECT COUNT(*) AS c FROM bookings").get("c")));
        stats.put("revenue", num(db.queryOne("SELECT COALESCE(SUM(amount),0) AS total FROM payments WHERE status='completed'").get("total")));
        stats.put("pendingDocs", num(db.queryOne("SELECT COUNT(*) AS c FROM worker_documents WHERE status='pending'").get("c")));
        stats.put("openComplaints", num(db.queryOne("SELECT COUNT(*) AS c FROM complaints WHERE status IN ('open','reviewing')").get("c")));
        return ApiResponses.ok("stats", stats);
    }

    @GetMapping("/analytics")
    public Map<String, Object> analytics() {
        List<Map<String, Object>> orderRows = db.query(
                """
                SELECT b.id, b.status, b.scheduled_at, b.total_price, b.address, b.created_at,
                       s.title AS service_title,
                       cust.id AS customer_id, cust.full_name AS customer_name, cust.email AS customer_email,
                       cust.phone AS customer_phone,
                       w.id AS worker_id, wu.full_name AS worker_name, wu.email AS worker_email,
                       wu.phone AS worker_phone
                FROM bookings b
                JOIN services s ON s.id = b.service_id
                JOIN users cust ON cust.id = b.customer_id
                JOIN workers w ON w.id = b.worker_id
                JOIN users wu ON wu.id = w.user_id
                ORDER BY FIELD(b.status,'pending','accepted','in_progress','completed','rejected','cancelled'), b.id DESC
                LIMIT 500
                """
        );
        Map<String, List<Map<String, Object>>> ordersByStatus = new LinkedHashMap<>();
        for (String s : List.of("pending", "accepted", "in_progress", "completed", "rejected", "cancelled")) {
            ordersByStatus.put(s, new ArrayList<>());
        }
        for (Map<String, Object> row : orderRows) {
            String st = String.valueOf(row.get("status"));
            if (ordersByStatus.containsKey(st)) {
                ordersByStatus.get(st).add(row);
            }
        }
        Map<String, Object> res = ApiResponses.ok();
        res.put("bookingsByStatus", db.query("SELECT status, COUNT(*) AS count FROM bookings GROUP BY status"));
        res.put("revenueByMonth", db.query(
                "SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, SUM(amount) AS total FROM payments WHERE status='completed' GROUP BY month ORDER BY month DESC LIMIT 12"
        ));
        res.put("topWorkers", db.query(
                """
                SELECT w.id, u.full_name, w.rating_avg, w.rating_count FROM workers w
                JOIN users u ON u.id = w.user_id ORDER BY w.rating_avg DESC, w.rating_count DESC LIMIT 10
                """
        ));
        res.put("ordersByStatus", ordersByStatus);
        return res;
    }

    @GetMapping("/users")
    public Map<String, Object> users(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int limit) {
        page = Math.max(1, page);
        limit = Math.min(100, limit);
        int offset = (page - 1) * limit;
        List<Map<String, Object>> rows = db.query(
                "SELECT id, email, role, full_name, phone, city, is_banned, suspended_until, created_at FROM users ORDER BY id DESC LIMIT ? OFFSET ?",
                limit, offset
        );
        Map<String, Object> res = ApiResponses.ok("data", rows);
        res.put("total", db.queryOne("SELECT COUNT(*) AS c FROM users").get("c"));
        res.put("page", page);
        res.put("limit", limit);
        return res;
    }

    @GetMapping("/workers")
    public Map<String, Object> workers() {
        return ApiResponses.ok("data", db.query(
                "SELECT w.*, u.email, u.full_name, u.city, u.is_banned FROM workers w JOIN users u ON u.id = w.user_id ORDER BY w.id DESC"
        ));
    }

    @GetMapping("/documents")
    public Map<String, Object> documents() {
        return ApiResponses.ok("data", documents.publicMetadata(db.query(
                """
                SELECT d.*, u.full_name AS worker_name, w.is_verified AS worker_is_verified FROM worker_documents d
                JOIN workers w ON w.id = d.worker_id
                JOIN users u ON u.id = w.user_id
                ORDER BY FIELD(d.status,'pending') DESC, d.id DESC
                """
        )));
    }

    // One approved document is sufficient; a rejected document never cancels another approval.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PatchMapping("/documents/{id}")
    public Map<String, Object> verifyDoc(@PathVariable long id, @RequestBody ReviewDocumentRequest body) {
        AuthUser auth = SecurityUtils.currentUser();
        String status = String.valueOf(body.getStatus());
        if (!List.of("approved", "rejected").contains(status)) {
            throw new ApiException(400, "Invalid status");
        }
        Map<String, Object> doc = db.queryOne("SELECT * FROM worker_documents WHERE id = ?", id);
        if (doc == null) {
            throw new ApiException(404, "Document not found");
        }
        // Serialize decisions for this worker, including reviews of different documents.
        db.queryOne("SELECT id FROM workers WHERE id = ? FOR UPDATE", doc.get("worker_id"));
        db.run("UPDATE worker_documents SET status = ?, admin_note = ?, reviewed_by = ?, reviewed_at = NOW() WHERE id = ?",
                status, body.getAdminNote(), auth.id(), id);
        boolean verified = RowMaps.asLong(db.queryOne(
                "SELECT COUNT(*) AS count FROM worker_documents WHERE worker_id = ? AND status = 'approved'",
                doc.get("worker_id")).get("count")) > 0;
        db.run("UPDATE workers SET verified_at = CASE WHEN ? THEN COALESCE(verified_at, NOW()) ELSE NULL END, is_verified = ? WHERE id = ?",
                verified, verified, doc.get("worker_id"));
        log(auth.id(), "verify_document", "worker_document", id, Map.of("status", status));
        Map<String, Object> result = ApiResponses.msg("Document " + status + ". Worker is " + (verified ? "verified." : "not verified."));
        result.put("worker_is_verified", verified);
        return result;
    }

    @GetMapping("/bookings")
    public Map<String, Object> bookings() {
        return ApiResponses.ok("data", db.query(
                """
                SELECT b.*, s.title AS service_title, c.name AS category_name,
                       cust.full_name AS customer_name, cust.email AS customer_email, cust.phone AS customer_phone,
                       wu.full_name AS worker_name, wu.email AS worker_email, wu.phone AS worker_phone
                FROM bookings b
                JOIN services s ON s.id = b.service_id
                JOIN categories c ON c.id = s.category_id
                JOIN users cust ON cust.id = b.customer_id
                JOIN workers w ON w.id = b.worker_id
                JOIN users wu ON wu.id = w.user_id
                ORDER BY b.id DESC LIMIT 200
                """
        ));
    }

    @GetMapping("/payments")
    public Map<String, Object> payments() {
        return ApiResponses.ok("data", db.query(
                """
                SELECT p.*, s.title AS service_title, b.status AS booking_status,
                       u.full_name AS payer_name, u.email AS payer_email
                FROM payments p
                JOIN bookings b ON b.id = p.booking_id
                JOIN services s ON s.id = b.service_id
                JOIN users u ON u.id = p.payer_id
                ORDER BY p.id DESC LIMIT 200
                """
        ));
    }

    @GetMapping("/reviews")
    public Map<String, Object> reviews() {
        return ApiResponses.ok("data", db.query(
                """
                SELECT r.*, u.full_name AS reviewer_name, s.title AS service_title, wu.full_name AS worker_name
                FROM reviews r
                JOIN users u ON u.id = r.reviewer_id
                JOIN bookings b ON b.id = r.booking_id
                JOIN services s ON s.id = b.service_id
                JOIN workers w ON w.id = r.worker_id
                JOIN users wu ON wu.id = w.user_id
                ORDER BY r.id DESC LIMIT 200
                """
        ));
    }

    @GetMapping("/services")
    public Map<String, Object> services() {
        return ApiResponses.ok("data", db.query(
                """
                SELECT s.*, c.name AS category_name, u.full_name AS worker_name FROM services s
                JOIN categories c ON c.id = s.category_id
                JOIN workers w ON w.id = s.worker_id
                JOIN users u ON u.id = w.user_id
                ORDER BY s.id DESC LIMIT 200
                """
        ));
    }

    @PatchMapping("/users/{id}/ban")
    public Map<String, Object> ban(@PathVariable long id, @RequestBody UpdateUserBanRequest body) {
        AuthUser auth = SecurityUtils.currentUser();
        Map<String, Object> target = db.queryOne("SELECT id, role, full_name, email FROM users WHERE id = ?", id);
        if (target == null) {
            throw new ApiException(404, "User not found");
        }
        if ("admin".equals(String.valueOf(target.get("role")))) {
            throw new ApiException(400, "Cannot ban admin accounts");
        }
        if (id == auth.id()) {
            throw new ApiException(400, "Cannot ban your own account");
        }
        int banned = RowMaps.asBool(body.getIsBanned()) ? 1 : 0;
        db.run("UPDATE users SET is_banned = ? WHERE id = ?", banned, id);
        log(auth.id(), "ban_user", "user", id, Map.of("is_banned", banned == 1));
        String name = String.valueOf(target.get("full_name"));
        Map<String, Object> res = ApiResponses.msg(banned == 1 ? name + " has been banned" : name + " has been unbanned");
        res.put("is_banned", banned == 1);
        return res;
    }

    @PatchMapping("/users/{id}/suspend")
    public Map<String, Object> suspend(@PathVariable long id, @RequestBody SuspendUserRequest body) {
        AuthUser auth = SecurityUtils.currentUser();
        db.run("UPDATE users SET suspended_until = ? WHERE id = ?", body.getUntil(), id);
        log(auth.id(), "suspend_user", "user", id, Map.of("until", String.valueOf(body.getUntil())));
        return ApiResponses.ok();
    }

    @GetMapping("/complaints")
    public Map<String, Object> complaints() {
        return ApiResponses.ok("data", db.query(
                "SELECT c.*, u.full_name AS reporter_name FROM complaints c JOIN users u ON u.id = c.reporter_id ORDER BY c.id DESC"
        ));
    }

    @PatchMapping("/complaints/{id}")
    public Map<String, Object> updateComplaint(@PathVariable long id, @RequestBody UpdateComplaintRequest body) {
        AuthUser auth = SecurityUtils.currentUser();
        String status = String.valueOf(body.getStatus());
        if (!List.of("open", "reviewing", "resolved", "dismissed").contains(status)) {
            throw new ApiException(400, "Invalid status");
        }
        db.run("UPDATE complaints SET status = ?, resolution_note = ? WHERE id = ?",
                status, body.getResolutionNote(), id);
        log(auth.id(), "complaint_update", "complaint", id, Map.of("status", status));
        return ApiResponses.ok();
    }

    private void log(long adminId, String action, String targetType, long targetId, Map<String, Object> details) {
        db.run("INSERT INTO admin_logs (admin_id, action, target_type, target_id, details) VALUES (?, ?, ?, ?, ?)",
                adminId, action, targetType, targetId, RowMaps.json(details));
    }

    private static Object num(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n;
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return 0;
        }
    }
}
