package com.worksure.web;

import com.worksure.web.request.CreateComplaintRequest;

import com.worksure.db.Db;
import com.worksure.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {
    private final Db db;

    public ComplaintController(Db db) {
        this.db = db;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateComplaintRequest body) {
        String subject = String.valueOf(body.getSubject()).trim();
        String message = String.valueOf(body.getMessage()).trim();
        if (subject.isBlank() || message.isBlank()) {
            throw new ApiException(400, "subject and message are required");
        }
        long id = db.insert(
                "INSERT INTO complaints (reporter_id, subject_user_id, booking_id, subject, message) VALUES (?, ?, ?, ?, ?)",
                SecurityUtils.currentUser().id(),
                body.getSubjectUserId(),
                body.getBookingId(),
                subject,
                message
        );
        return ResponseEntity.status(201).body(ApiResponses.ok("id", id));
    }

    @GetMapping("/mine")
    public Map<String, Object> mine() {
        return ApiResponses.ok("data", db.query(
                "SELECT * FROM complaints WHERE reporter_id = ? ORDER BY id DESC",
                SecurityUtils.currentUser().id()
        ));
    }
}
