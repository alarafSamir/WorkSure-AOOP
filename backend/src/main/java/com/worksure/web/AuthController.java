package com.worksure.web;

import com.worksure.web.request.RegisterRequest;
import com.worksure.web.request.LoginRequest;
import com.worksure.web.request.ForgotPasswordRequest;
import com.worksure.web.request.ResetPasswordRequest;
import com.worksure.web.request.UpdateFcmTokenRequest;

import com.worksure.db.Db;
import com.worksure.security.AuthUser;
import com.worksure.security.JwtService;
import com.worksure.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final Db db;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(Db db, PasswordEncoder encoder, JwtService jwt) {
        this.db = db;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest body) {
        String email = str(body.getEmail()).toLowerCase().trim();
        String password = str(body.getPassword());
        String fullName = str(body.getFullName()).trim();
        String phone = emptyToNull(str(body.getPhone()));
        String role = str(body.getRole());
        if (role.isBlank()) {
            role = "customer";
        }
        if (!email.contains("@")) {
            throw new ApiException(400, "Invalid email");
        }
        if (password.length() < 8) {
            throw new ApiException(400, "Password must be at least 8 characters");
        }
        if (fullName.isBlank()) {
            throw new ApiException(400, "full_name is required");
        }
        if ("admin".equals(role)) {
            throw new ApiException(403, "Cannot self-register as admin");
        }
        if (!"customer".equals(role) && !"worker".equals(role)) {
            throw new ApiException(400, "Invalid role");
        }
        if (db.queryOne("SELECT id FROM users WHERE email = ?", email) != null) {
            throw new ApiException(409, "Email already registered");
        }
        String hash = encoder.encode(password);
        long userId = db.insert(
                "INSERT INTO users (email, password_hash, role, full_name, phone) VALUES (?, ?, ?, ?, ?)",
                email, hash, role, fullName, phone
        );
        if ("worker".equals(role)) {
            db.run("INSERT INTO workers (user_id, headline, bio, hourly_rate) VALUES (?, ?, ?, ?)",
                    userId, "New WorkSure professional", "", 0);
        }
        Map<String, Object> user = db.queryOne(
                "SELECT id, email, role, full_name, phone, avatar_url, city FROM users WHERE id = ?",
                userId
        );
        Map<String, Object> res = ApiResponses.ok("user", user);
        res.put("token", jwt.sign(userId, role));
        return ResponseEntity.status(201).body(res);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest body) {
        String email = str(body.getEmail()).toLowerCase().trim();
        String password = str(body.getPassword());
        if (email.isBlank() || password.isBlank()) {
            throw new ApiException(401, "Invalid credentials");
        }
        Map<String, Object> user = db.queryOne(
                "SELECT id, email, password_hash, role, full_name, phone, avatar_url, city, is_banned, suspended_until FROM users WHERE email = ?",
                email
        );
        if (user == null || !encoder.matches(password, String.valueOf(user.get("password_hash")))) {
            throw new ApiException(401, "Invalid credentials");
        }
        if (com.worksure.util.RowMaps.asBool(user.get("is_banned"))) {
            throw new ApiException(403, "Account is banned");
        }
        Object suspended = user.get("suspended_until");
        if (suspended != null && isFuture(String.valueOf(suspended))) {
            throw new ApiException(403, "Account is suspended");
        }
        user.remove("password_hash");
        Map<String, Object> res = ApiResponses.ok("user", user);
        res.put("token", jwt.sign(com.worksure.util.RowMaps.asLong(user.get("id")), String.valueOf(user.get("role"))));
        return res;
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        AuthUser auth = SecurityUtils.currentUser();
        Map<String, Object> user = db.queryOne(
                "SELECT id, email, role, full_name, phone, avatar_url, address, city, country, latitude, longitude, created_at FROM users WHERE id = ?",
                auth.id()
        );
        Map<String, Object> worker = null;
        if ("worker".equals(auth.role())) {
            worker = db.queryOne("SELECT * FROM workers WHERE user_id = ?", auth.id());
        }
        Map<String, Object> res = ApiResponses.ok("user", user);
        res.put("worker", worker);
        return res;
    }

    @PostMapping("/forgot-password")
    public Map<String, Object> forgot(@RequestBody ForgotPasswordRequest body) {
        String email = str(body.getEmail()).toLowerCase().trim();
        Map<String, Object> generic = ApiResponses.msg("If an account exists, reset instructions were sent.");
        Map<String, Object> user = db.queryOne("SELECT id FROM users WHERE email = ?", email);
        if (user == null) {
            return generic;
        }
        byte[] buf = new byte[32];
        new SecureRandom().nextBytes(buf);
        String token = HexFormat.of().formatHex(buf);
        Instant expires = Instant.now().plus(1, ChronoUnit.HOURS);
        db.run("UPDATE users SET reset_password_token = ?, reset_password_expires = ? WHERE id = ?",
                token, java.sql.Timestamp.from(expires), user.get("id"));
        String link = "http://localhost:5173/reset-password?token=" + token;
        System.out.println("[WorkSure] Password reset for " + email + ": " + link);
        generic.put("devResetLink", link);
        return generic;
    }

    @PostMapping("/reset-password")
    public Map<String, Object> reset(@RequestBody ResetPasswordRequest body) {
        String token = str(body.getToken());
        String password = str(body.getPassword());
        if (token.isBlank()) {
            throw new ApiException(400, "Invalid or expired token");
        }
        if (password.length() < 8) {
            throw new ApiException(400, "Password must be at least 8 characters");
        }
        Map<String, Object> user = db.queryOne(
                "SELECT id FROM users WHERE reset_password_token = ? AND reset_password_expires > NOW()",
                token
        );
        if (user == null) {
            throw new ApiException(400, "Invalid or expired token");
        }
        db.run("UPDATE users SET password_hash = ?, reset_password_token = NULL, reset_password_expires = NULL WHERE id = ?",
                encoder.encode(password), user.get("id"));
        return ApiResponses.msg("Password updated. You can sign in now.");
    }

    @PatchMapping("/fcm-token")
    public Map<String, Object> fcm(@RequestBody UpdateFcmTokenRequest body) {
        AuthUser auth = SecurityUtils.currentUser();
        db.run("UPDATE users SET fcm_token = ? WHERE id = ?", emptyToNull(str(body.getFcmToken())), auth.id());
        return ApiResponses.ok();
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static boolean isFuture(String raw) {
        try {
            Instant i = Instant.parse(raw);
            return i.isAfter(Instant.now());
        } catch (Exception e) {
            return false;
        }
    }
}
