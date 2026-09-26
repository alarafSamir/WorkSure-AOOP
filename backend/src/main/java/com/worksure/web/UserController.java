package com.worksure.web;

import com.worksure.web.request.UpdateProfileRequest;

import com.worksure.db.Db;
import com.worksure.security.AuthUser;
import com.worksure.security.SecurityUtils;
import com.worksure.storage.UploadService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final Db db;
    private final UploadService uploads;

    public UserController(Db db, UploadService uploads) {
        this.db = db;
        this.uploads = uploads;
    }

    @GetMapping("/profile")
    public Map<String, Object> getProfile() {
        AuthUser auth = SecurityUtils.currentUser();
        Map<String, Object> user = db.queryOne(
                "SELECT id, email, role, full_name, phone, avatar_url, address, city, country, latitude, longitude, created_at FROM users WHERE id = ?",
                auth.id()
        );
        return ApiResponses.ok("user", user);
    }

    @PatchMapping("/profile")
    public Map<String, Object> updateProfile(@RequestBody UpdateProfileRequest body) {
        AuthUser auth = SecurityUtils.currentUser();
        StringBuilder sql = new StringBuilder("UPDATE users SET ");
        java.util.List<Object> params = new java.util.ArrayList<>();
        int n = 0;
        if (body.hasFullName()) {
            if (n++ > 0) sql.append(", ");
            sql.append("full_name = ?");
            params.add(body.getFullName());
        }
        if (body.hasPhone()) {
            if (n++ > 0) sql.append(", ");
            sql.append("phone = ?");
            params.add(body.getPhone());
        }
        if (body.hasAddress()) {
            if (n++ > 0) sql.append(", ");
            sql.append("address = ?");
            params.add(body.getAddress());
        }
        if (body.hasCity()) {
            if (n++ > 0) sql.append(", ");
            sql.append("city = ?");
            params.add(body.getCity());
        }
        if (body.hasCountry()) {
            if (n++ > 0) sql.append(", ");
            sql.append("country = ?");
            params.add(body.getCountry());
        }
        if (body.hasLatitude()) {
            if (n++ > 0) sql.append(", ");
            sql.append("latitude = ?");
            params.add(body.getLatitude());
        }
        if (body.hasLongitude()) {
            if (n++ > 0) sql.append(", ");
            sql.append("longitude = ?");
            params.add(body.getLongitude());
        }
        if (n == 0) {
            throw new ApiException(400, "No fields to update");
        }
        sql.append(" WHERE id = ?");
        params.add(auth.id());
        db.run(sql.toString(), params.toArray());
        Map<String, Object> user = db.queryOne(
                "SELECT id, email, role, full_name, phone, avatar_url, address, city, country, latitude, longitude FROM users WHERE id = ?",
                auth.id()
        );
        return ApiResponses.ok("user", user);
    }

    @PostMapping("/avatar")
    public Map<String, Object> avatar(@RequestParam("avatar") MultipartFile file) {
        AuthUser auth = SecurityUtils.currentUser();
        String url = uploads.store(file);
        db.run("UPDATE users SET avatar_url = ? WHERE id = ?", url, auth.id());
        return ApiResponses.ok("avatar_url", url);
    }
}
