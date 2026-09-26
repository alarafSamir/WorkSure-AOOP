package com.worksure.web;

import com.worksure.web.request.AddWishlistItemRequest;

import com.worksure.db.Db;
import com.worksure.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {
    private final Db db;

    public WishlistController(Db db) {
        this.db = db;
    }

    @GetMapping
    public Map<String, Object> list() {
        requireCustomer();
        return ApiResponses.ok("items", db.query(
                """
                SELECT w.created_at, s.*, c.name AS category_name, u.full_name AS worker_name
                FROM wishlist w
                JOIN services s ON s.id = w.service_id
                JOIN categories c ON c.id = s.category_id
                JOIN workers wr ON wr.id = s.worker_id
                JOIN users u ON u.id = wr.user_id
                WHERE w.user_id = ? ORDER BY w.created_at DESC
                """,
                SecurityUtils.currentUser().id()
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> add(@RequestBody AddWishlistItemRequest body) {
        requireCustomer();
        Long serviceId = body.getServiceId();
        if (serviceId == null) {
            throw new ApiException(400, "service_id is required");
        }
        db.run("INSERT IGNORE INTO wishlist (user_id, service_id) VALUES (?, ?)",
                SecurityUtils.currentUser().id(), serviceId);
        return ResponseEntity.status(201).body(ApiResponses.ok());
    }

    @DeleteMapping("/{serviceId}")
    public Map<String, Object> remove(@PathVariable long serviceId) {
        requireCustomer();
        db.run("DELETE FROM wishlist WHERE user_id = ? AND service_id = ?",
                SecurityUtils.currentUser().id(), serviceId);
        return ApiResponses.ok();
    }

    private void requireCustomer() {
        SecurityUtils.requireRole(SecurityUtils.currentUser(), "customer", "admin");
    }
}
