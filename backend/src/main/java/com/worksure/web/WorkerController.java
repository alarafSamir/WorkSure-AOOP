package com.worksure.web;

import com.worksure.web.request.UpdateWorkerProfileRequest;
import com.worksure.web.request.RequestJson;

import com.worksure.db.Db;
import com.worksure.security.AuthUser;
import com.worksure.security.SecurityUtils;
import com.worksure.storage.VerificationDocuments;
import com.worksure.util.Categories;
import com.worksure.util.RowMaps;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workers")
public class WorkerController {
    private final Db db;
    private final Categories categories;
    private final VerificationDocuments uploads;

    public WorkerController(Db db, Categories categories, VerificationDocuments uploads) {
        this.db = db;
        this.categories = categories;
        this.uploads = uploads;
    }

    @GetMapping("/public")
    public Map<String, Object> listPublic(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) String verified,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double maxPrice
    ) {
        page = Math.max(1, page);
        limit = Math.min(100, Math.max(1, limit));
        int offset = (page - 1) * limit;
        StringBuilder where = new StringBuilder("WHERE u.is_banned = 0");
        List<Object> params = new ArrayList<>();
        if (city != null && !city.isBlank()) {
            where.append(" AND u.city LIKE ?");
            params.add("%" + city + "%");
        }
        if (minRating != null) {
            where.append(" AND w.rating_avg >= ?");
            params.add(minRating);
        }
        if ("1".equals(verified) || "true".equalsIgnoreCase(verified)) {
            where.append(" AND w.is_verified = 1");
        }
        if (q != null && !q.isBlank()) {
            where.append(" AND (u.full_name LIKE ? OR w.headline LIKE ?)");
            String like = "%" + q + "%";
            params.add(like);
            params.add(like);
        }
        if (category != null && !category.isBlank()) {
            List<Long> ids = categories.resolveCategoryIds(category);
            if (ids == null || ids.isEmpty()) {
                where.append(" AND 1 = 0");
            } else {
                where.append(" AND EXISTS (SELECT 1 FROM services sv WHERE sv.worker_id = w.id AND sv.is_active = 1 AND sv.category_id IN (")
                        .append(categories.inPlaceholders(ids.size())).append("))");
                params.addAll(ids);
            }
        }
        if (maxPrice != null) {
            where.append(" AND w.hourly_rate <= ?");
            params.add(maxPrice);
        }
        Map<String, Object> countRow = db.queryOne(
                "SELECT COUNT(*) AS total FROM workers w JOIN users u ON u.id = w.user_id " + where,
                params.toArray()
        );
        long total = RowMaps.asLong(countRow.get("total"));
        List<Object> listParams = new ArrayList<>(params);
        listParams.add(limit);
        listParams.add(offset);
        List<Map<String, Object>> rows = db.query(
                """
                SELECT w.id, w.headline, w.bio, w.hourly_rate, w.rating_avg, w.rating_count, w.is_verified, w.years_experience,
                       u.id AS user_id, u.full_name, u.city, u.avatar_url
                FROM workers w
                JOIN users u ON u.id = w.user_id
                """ + where + " ORDER BY w.is_verified DESC, w.rating_avg DESC, w.id DESC LIMIT ? OFFSET ?",
                listParams.toArray()
        );
        Map<String, Object> res = ApiResponses.ok("data", rows);
        res.put("pagination", Map.of("page", page, "limit", limit, "total", total, "pages", Math.max(1, (int) Math.ceil(total / (double) limit))));
        return res;
    }

    @GetMapping("/public/{id}")
    public Map<String, Object> getPublic(@PathVariable long id) {
        Map<String, Object> worker = db.queryOne(
                """
                SELECT w.*, u.full_name, u.city, u.avatar_url, u.created_at AS member_since
                FROM workers w JOIN users u ON u.id = w.user_id WHERE w.id = ? AND u.is_banned = 0
                """,
                id
        );
        if (worker == null) {
            throw new ApiException(404, "Worker not found");
        }
        Map<String, Object> res = ApiResponses.ok("worker", worker);
        res.put("services", db.query(
                """
                SELECT s.*, c.name AS category_name, c.slug AS category_slug
                FROM services s JOIN categories c ON c.id = s.category_id
                WHERE s.worker_id = ? AND s.is_active = 1
                """,
                id
        ));
        res.put("reviews", db.query(
                """
                SELECT r.*, u.full_name AS reviewer_name, u.avatar_url AS reviewer_avatar
                FROM reviews r JOIN users u ON u.id = r.reviewer_id
                WHERE r.worker_id = ? ORDER BY r.id DESC LIMIT 20
                """,
                id
        ));
        return res;
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        AuthUser auth = SecurityUtils.currentUser();
        SecurityUtils.requireRole(auth, "worker");
        Map<String, Object> worker = db.queryOne(
                """
                SELECT w.*, u.full_name, u.email, u.phone, u.avatar_url, u.city
                FROM workers w JOIN users u ON u.id = w.user_id WHERE u.id = ?
                """,
                auth.id()
        );
        if (worker == null) {
            throw new ApiException(404, "Worker profile not found");
        }
        return ApiResponses.ok("worker", worker);
    }

    @GetMapping("/me/services")
    public Map<String, Object> myServices() {
        AuthUser auth = SecurityUtils.currentUser();
        SecurityUtils.requireRole(auth, "worker");
        Map<String, Object> worker = requireWorker(auth.id());
        return ApiResponses.ok("services", db.query(
                """
                SELECT s.*, c.name AS category_name FROM services s
                JOIN categories c ON c.id = s.category_id
                WHERE s.worker_id = ? ORDER BY s.id DESC
                """,
                worker.get("id")
        ));
    }

    @PatchMapping("/me")
    public Map<String, Object> updateMe(@RequestBody UpdateWorkerProfileRequest body) {
        AuthUser auth = SecurityUtils.currentUser();
        SecurityUtils.requireRole(auth, "worker");
        Map<String, Object> worker = db.queryOne("SELECT id FROM workers WHERE user_id = ?", auth.id());
        if (worker == null) {
            throw new ApiException(404, "Worker profile not found");
        }
        StringBuilder sql = new StringBuilder("UPDATE workers SET ");
        List<Object> params = new ArrayList<>();
        int n = 0;
        if (body.hasHeadline()) {
            if (n++ > 0) sql.append(", ");
            sql.append("headline = ?");
            params.add(body.getHeadline());
        }
        if (body.hasBio()) {
            if (n++ > 0) sql.append(", ");
            sql.append("bio = ?");
            params.add(body.getBio());
        }
        if (body.hasHourlyRate()) {
            if (n++ > 0) sql.append(", ");
            sql.append("hourly_rate = ?");
            params.add(body.getHourlyRate());
        }
        if (body.hasServiceRadiusKm()) {
            if (n++ > 0) sql.append(", ");
            sql.append("service_radius_km = ?");
            params.add(body.getServiceRadiusKm());
        }
        if (body.hasYearsExperience()) {
            if (n++ > 0) sql.append(", ");
            sql.append("years_experience = ?");
            params.add(body.getYearsExperience());
        }
        if (body.hasAvailability()) {
            if (n++ > 0) sql.append(", ");
            sql.append("availability = ?");
            params.add(RequestJson.databaseValue(body.getAvailability()));
        }
        if (n > 0) {
            sql.append(" WHERE id = ?");
            params.add(worker.get("id"));
            db.run(sql.toString(), params.toArray());
        }
        Map<String, Object> updated = db.queryOne(
                """
                SELECT w.*, u.full_name, u.email, u.phone, u.avatar_url FROM workers w
                JOIN users u ON u.id = w.user_id WHERE w.id = ?
                """,
                worker.get("id")
        );
        return ApiResponses.ok("worker", updated);
    }

    @GetMapping("/me/documents")
    public Map<String, Object> documents() {
        AuthUser auth = SecurityUtils.currentUser();
        SecurityUtils.requireRole(auth, "worker");
        Map<String, Object> worker = requireWorker(auth.id());
        return ApiResponses.ok("documents", uploads.publicMetadata(db.query(
                "SELECT * FROM worker_documents WHERE worker_id = ? ORDER BY id DESC",
                worker.get("id")
        )));
    }

    @PostMapping("/me/documents")
    public ResponseEntity<Map<String, Object>> uploadDoc(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "doc_type", required = false) String docType
    ) {
        AuthUser auth = SecurityUtils.currentUser();
        SecurityUtils.requireRole(auth, "worker");
        Map<String, Object> worker = requireWorker(auth.id());
        String type = (docType == null || docType.isBlank()) ? "nid" : docType;
        if (!List.of("nid", "passport", "license", "certificate", "other").contains(type)) {
            throw new ApiException(400, "Invalid doc_type");
        }
        String url = uploads.store(file);
        db.run("INSERT INTO worker_documents (worker_id, doc_type, file_url, status) VALUES (?, ?, ?, 'pending')",
                worker.get("id"), type, url);
        return ResponseEntity.status(201).body(ApiResponses.msg("Document submitted for review"));
    }

    @GetMapping("/me/earnings")
    public Map<String, Object> earnings() {
        AuthUser auth = SecurityUtils.currentUser();
        SecurityUtils.requireRole(auth, "worker");
        Map<String, Object> worker = requireWorker(auth.id());
        Map<String, Object> paid = db.queryOne(
                """
                SELECT COALESCE(SUM(COALESCE(p.worker_payout, p.amount)),0) AS total,
                       COALESCE(SUM(p.platform_commission),0) AS platform_fees
                FROM payments p JOIN bookings b ON b.id = p.booking_id
                WHERE b.worker_id = ? AND p.status = 'completed'
                """,
                worker.get("id")
        );
        List<Map<String, Object>> monthly = db.query(
                """
                SELECT DATE_FORMAT(p.created_at, '%Y-%m') AS month,
                       SUM(COALESCE(p.worker_payout, p.amount)) AS total
                FROM payments p JOIN bookings b ON b.id = p.booking_id
                WHERE b.worker_id = ? AND p.status = 'completed' GROUP BY month ORDER BY month DESC LIMIT 12
                """,
                worker.get("id")
        );
        Map<String, Object> res = ApiResponses.ok();
        res.put("total", paid.get("total") == null ? 0 : Double.parseDouble(String.valueOf(paid.get("total"))));
        res.put("platform_fees", paid.get("platform_fees") == null ? 0 : Double.parseDouble(String.valueOf(paid.get("platform_fees"))));
        res.put("monthly", monthly);
        return res;
    }

    private Map<String, Object> requireWorker(long userId) {
        Map<String, Object> worker = db.queryOne("SELECT id FROM workers WHERE user_id = ?", userId);
        if (worker == null) {
            throw new ApiException(404, "Worker profile not found");
        }
        return worker;
    }
}
