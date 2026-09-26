package com.worksure.web;

import com.worksure.web.request.CreateServiceRequest;
import com.worksure.web.request.UpdateServiceRequest;
import com.worksure.web.request.RequestJson;

import com.worksure.db.Db;
import com.worksure.security.AuthUser;
import com.worksure.security.SecurityUtils;
import com.worksure.storage.UploadService;
import com.worksure.util.Categories;
import com.worksure.util.RowMaps;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
public class ServiceController {
    private final Db db;
    private final Categories categories;
    private final UploadService uploads;

    public ServiceController(Db db, Categories categories, UploadService uploads) {
        this.db = db;
        this.categories = categories;
        this.uploads = uploads;
    }

    @GetMapping("/categories")
    public Map<String, Object> categories() {
        List<Map<String, Object>> rows = db.query(
                "SELECT id, parent_id, name, slug, icon, description, image_url, sort_order FROM categories ORDER BY sort_order, name"
        );
        List<Map<String, Object>> majors = new ArrayList<>();
        for (Map<String, Object> major : rows) {
            if (major.get("parent_id") == null) {
                List<Map<String, Object>> subs = new ArrayList<>();
                long mid = RowMaps.asLong(major.get("id"));
                for (Map<String, Object> c : rows) {
                    if (c.get("parent_id") != null && RowMaps.asLong(c.get("parent_id")) == mid) {
                        subs.add(c);
                    }
                }
                Map<String, Object> m = new java.util.LinkedHashMap<>(major);
                m.put("subfeatures", subs);
                majors.add(m);
            }
        }
        Map<String, Object> res = ApiResponses.ok("majors", majors);
        res.put("categories", rows);
        return res;
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int limit,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) String verified
    ) {
        page = Math.max(1, page);
        limit = Math.min(50, Math.max(1, limit));
        int offset = (page - 1) * limit;
        StringBuilder where = new StringBuilder("WHERE s.is_active = 1 AND u.is_banned = 0");
        List<Object> params = new ArrayList<>();
        if (category != null && !category.isBlank()) {
            List<Long> ids = categories.resolveCategoryIds(category);
            if (ids == null || ids.isEmpty()) {
                where.append(" AND 1 = 0");
            } else {
                where.append(" AND c.id IN (").append(categories.inPlaceholders(ids.size())).append(")");
                params.addAll(ids);
            }
        }
        if (minPrice != null) {
            where.append(" AND s.base_price >= ?");
            params.add(minPrice);
        }
        if (maxPrice != null) {
            where.append(" AND s.base_price <= ?");
            params.add(maxPrice);
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q + "%";
            where.append(" AND (s.title LIKE ? OR s.description LIKE ? OR s.tags LIKE ?)");
            params.add(like);
            params.add(like);
            params.add(like);
        }
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
        Map<String, Object> countRow = db.queryOne(
                """
                SELECT COUNT(*) AS total FROM services s
                JOIN workers w ON w.id = s.worker_id
                JOIN users u ON u.id = w.user_id
                JOIN categories c ON c.id = s.category_id
                """ + where,
                params.toArray()
        );
        long total = RowMaps.asLong(countRow.get("total"));
        List<Object> listParams = new ArrayList<>(params);
        listParams.add(limit);
        listParams.add(offset);
        List<Map<String, Object>> rows = db.query(
                """
                SELECT s.*, c.name AS category_name, c.slug AS category_slug,
                       w.id AS worker_table_id, w.rating_avg AS worker_rating, w.is_verified,
                       u.full_name AS worker_name, u.city AS worker_city, u.avatar_url AS worker_avatar
                FROM services s
                JOIN workers w ON w.id = s.worker_id
                JOIN users u ON u.id = w.user_id
                JOIN categories c ON c.id = s.category_id
                """ + where + " ORDER BY w.is_verified DESC, w.rating_avg DESC, s.id DESC LIMIT ? OFFSET ?",
                listParams.toArray()
        );
        Map<String, Object> res = ApiResponses.ok("data", rows);
        res.put("pagination", Map.of("page", page, "limit", limit, "total", total, "pages", Math.max(1, (int) Math.ceil(total / (double) limit))));
        return res;
    }

    @GetMapping("/search/suggestions")
    public Map<String, Object> suggestions(@RequestParam(required = false) String q) {
        if (q == null || q.trim().length() < 2) {
            return ApiResponses.ok("suggestions", List.of());
        }
        String like = "%" + q.trim() + "%";
        return ApiResponses.ok("suggestions", db.query(
                """
                SELECT DISTINCT s.title, c.name AS category FROM services s
                JOIN categories c ON c.id = s.category_id
                WHERE s.is_active = 1 AND (s.title LIKE ? OR c.name LIKE ?) LIMIT 8
                """,
                like, like
        ));
    }

    @GetMapping("/recommendations")
    public Map<String, Object> recommendations() {
        AuthUser auth = SecurityUtils.currentUserOrNull();
        List<Object> categoryIds = new ArrayList<>();
        if (auth != null) {
            List<Map<String, Object>> hist = db.query(
                    """
                    SELECT DISTINCT s.category_id FROM bookings b
                    JOIN services s ON s.id = b.service_id
                    WHERE b.customer_id = ? ORDER BY b.id DESC LIMIT 5
                    """,
                    auth.id()
            );
            for (Map<String, Object> h : hist) {
                if (h.get("category_id") != null) {
                    categoryIds.add(h.get("category_id"));
                }
            }
        }
        String sql = """
                SELECT s.*, c.name AS category_name, w.rating_avg, u.full_name AS worker_name
                FROM services s
                JOIN categories c ON c.id = s.category_id
                JOIN workers w ON w.id = s.worker_id
                JOIN users u ON u.id = w.user_id
                WHERE s.is_active = 1 AND u.is_banned = 0
                """;
        List<Map<String, Object>> trending;
        if (!categoryIds.isEmpty()) {
            sql += " ORDER BY FIELD(s.category_id, " + categories.inPlaceholders(categoryIds.size()) + ") DESC, w.rating_avg DESC, RAND() LIMIT 8";
            trending = db.query(sql, categoryIds.toArray());
        } else {
            sql += " ORDER BY w.rating_avg DESC, RAND() LIMIT 8";
            trending = db.query(sql);
        }
        Map<String, Object> res = ApiResponses.ok("data", trending);
        res.put("note", "Heuristic mock — replace with ML ranker in production.");
        return res;
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable long id) {
        Map<String, Object> row = db.queryOne(
                """
                SELECT s.*, c.name AS category_name, c.slug AS category_slug,
                       w.id AS worker_id, w.headline AS worker_headline, w.rating_avg, w.is_verified, w.hourly_rate,
                       u.full_name AS worker_name, u.city AS worker_city, u.avatar_url AS worker_avatar, u.id AS worker_user_id
                FROM services s
                JOIN workers w ON w.id = s.worker_id
                JOIN users u ON u.id = w.user_id
                JOIN categories c ON c.id = s.category_id
                WHERE s.id = ? AND s.is_active = 1
                """,
                id
        );
        if (row == null) {
            throw new ApiException(404, "Service not found");
        }
        return ApiResponses.ok("service", row);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateServiceRequest body) {
        AuthUser auth = SecurityUtils.currentUser();
        SecurityUtils.requireRole(auth, "worker");
        Map<String, Object> worker = requireWorker(auth.id());
        String title = String.valueOf(body.getTitle()).trim();
        String description = String.valueOf(body.getDescription()).trim();
        Long categoryId = body.getCategoryId();
        java.math.BigDecimal price = body.getBasePrice();
        if (title.isBlank() || description.isBlank() || categoryId == null || price == null) {
            throw new ApiException(400, "category_id, title, description and base_price are required");
        }
        int duration = RowMaps.asInt(body.getDurationMinutes(), 60);
        String slug = slugify(title + "-" + worker.get("id") + "-" + System.currentTimeMillis());
        String images = body.getImages() != null ? RequestJson.databaseValue(body.getImages()) : null;
        String tags = body.getTags();
        long id = db.insert(
                """
                INSERT INTO services (worker_id, category_id, title, slug, description, base_price, duration_minutes, images, tags)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                worker.get("id"), categoryId, title, slug, description, price, duration, images, tags
        );
        return ResponseEntity.status(201).body(ApiResponses.ok("service", db.queryOne("SELECT * FROM services WHERE id = ?", id)));
    }

    @PatchMapping("/{id}")
    public Map<String, Object> update(@PathVariable long id, @RequestBody UpdateServiceRequest body) {
        AuthUser auth = SecurityUtils.currentUser();
        SecurityUtils.requireRole(auth, "worker");
        Map<String, Object> worker = requireWorker(auth.id());
        Map<String, Object> svc = db.queryOne("SELECT * FROM services WHERE id = ? AND worker_id = ?", id, worker.get("id"));
        if (svc == null) {
            throw new ApiException(404, "Service not found");
        }
        StringBuilder sql = new StringBuilder("UPDATE services SET ");
        List<Object> params = new ArrayList<>();
        int n = 0;
        if (body.hasTitle()) {
            if (n++ > 0) sql.append(", ");
            sql.append("title = ?");
            params.add(body.getTitle());
        }
        if (body.hasDescription()) {
            if (n++ > 0) sql.append(", ");
            sql.append("description = ?");
            params.add(body.getDescription());
        }
        if (body.hasBasePrice()) {
            if (n++ > 0) sql.append(", ");
            sql.append("base_price = ?");
            params.add(body.getBasePrice());
        }
        if (body.hasDurationMinutes()) {
            if (n++ > 0) sql.append(", ");
            sql.append("duration_minutes = ?");
            params.add(body.getDurationMinutes());
        }
        if (body.hasTags()) {
            if (n++ > 0) sql.append(", ");
            sql.append("tags = ?");
            params.add(body.getTags());
        }
        if (body.hasIsActive()) {
            if (n++ > 0) sql.append(", ");
            sql.append("is_active = ?");
            params.add(RowMaps.asBool(body.getIsActive()) ? 1 : 0);
        }
        if (body.hasImages()) {
            if (n++ > 0) sql.append(", ");
            sql.append("images = ?");
            params.add(RequestJson.databaseValue(body.getImages()));
        }
        if (n > 0) {
            sql.append(" WHERE id = ?");
            params.add(id);
            db.run(sql.toString(), params.toArray());
        }
        return ApiResponses.ok("service", db.queryOne("SELECT * FROM services WHERE id = ?", id));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable long id) {
        AuthUser auth = SecurityUtils.currentUser();
        SecurityUtils.requireRole(auth, "worker");
        Map<String, Object> worker = requireWorker(auth.id());
        db.run("DELETE FROM services WHERE id = ? AND worker_id = ?", id, worker.get("id"));
        return ApiResponses.ok();
    }

    @PostMapping("/{id}/images")
    public Map<String, Object> images(@PathVariable long id, @RequestParam("images") MultipartFile[] files) {
        AuthUser auth = SecurityUtils.currentUser();
        SecurityUtils.requireRole(auth, "worker");
        Map<String, Object> worker = requireWorker(auth.id());
        Map<String, Object> svc = db.queryOne("SELECT * FROM services WHERE id = ? AND worker_id = ?", id, worker.get("id"));
        if (svc == null) {
            throw new ApiException(404, "Service not found");
        }
        if (files == null || files.length == 0) {
            throw new ApiException(400, "No files");
        }
        List<Object> images = new ArrayList<>();
        Object existing = svc.get("images");
        if (existing instanceof List<?> list) {
            images.addAll(list);
        }
        for (MultipartFile f : files) {
            images.add(uploads.store(f));
        }
        db.run("UPDATE services SET images = ? WHERE id = ?", RowMaps.json(images), id);
        return ApiResponses.ok("service", db.queryOne("SELECT * FROM services WHERE id = ?", id));
    }

    private Map<String, Object> requireWorker(long userId) {
        Map<String, Object> worker = db.queryOne("SELECT id FROM workers WHERE user_id = ?", userId);
        if (worker == null) {
            throw new ApiException(403, "Worker account required");
        }
        return worker;
    }

    private static String slugify(String text) {
        String s = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-)$", "");
        return s.length() > 200 ? s.substring(0, 200) : s;
    }
}
