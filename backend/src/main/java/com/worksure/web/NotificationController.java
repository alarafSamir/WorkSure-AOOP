package com.worksure.web;

import com.worksure.db.Db;
import com.worksure.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final Db db;

    public NotificationController(Db db) {
        this.db = db;
    }

    @GetMapping
    public Map<String, Object> list() {
        return ApiResponses.ok("notifications", db.query(
                "SELECT * FROM notifications WHERE user_id = ? ORDER BY id DESC LIMIT 100",
                SecurityUtils.currentUser().id()
        ));
    }

    @PatchMapping("/{id}/read")
    public Map<String, Object> markRead(@PathVariable long id) {
        db.run("UPDATE notifications SET is_read = 1 WHERE id = ? AND user_id = ?", id, SecurityUtils.currentUser().id());
        return ApiResponses.ok();
    }

    @PostMapping("/read-all")
    public Map<String, Object> markAll() {
        db.run("UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0", SecurityUtils.currentUser().id());
        return ApiResponses.ok();
    }
}
