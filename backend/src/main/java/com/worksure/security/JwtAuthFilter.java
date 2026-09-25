package com.worksure.security;

import com.worksure.db.Db;
import com.worksure.util.RowMaps;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final Db db;

    public JwtAuthFilter(JwtService jwtService, Db db) {
        this.jwtService = jwtService;
        this.db = db;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token);
                long userId = Long.parseLong(claims.getSubject());
                Map<String, Object> user = db.queryOne(
                        "SELECT id, email, role, full_name, is_banned, suspended_until FROM users WHERE id = ? AND is_active = 1",
                        userId
                );
                if (user != null) {
                    boolean banned = RowMaps.asBool(user.get("is_banned"));
                    Object suspended = user.get("suspended_until");
                    boolean isSuspended = false;
                    if (suspended != null) {
                        try {
                            Instant until = Instant.parse(String.valueOf(suspended).replace(' ', 'T') + (String.valueOf(suspended).endsWith("Z") ? "" : "Z"));
                            isSuspended = until.isAfter(Instant.now());
                        } catch (Exception ignored) {
                            try {
                                isSuspended = java.sql.Timestamp.valueOf(String.valueOf(suspended).replace('T', ' ').replace("Z", "").substring(0, 19)).toInstant().isAfter(Instant.now());
                            } catch (Exception ignored2) {
                                isSuspended = false;
                            }
                        }
                    }
                    if (!banned && !isSuspended) {
                        AuthUser principal = new AuthUser(
                                RowMaps.asLong(user.get("id")),
                                String.valueOf(user.get("email")),
                                String.valueOf(user.get("role")),
                                String.valueOf(user.get("full_name"))
                        );
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().toUpperCase()))
                        );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception ignored) {
                // Invalid token: continue unauthenticated so permitAll endpoints still work
            }
        }
        filterChain.doFilter(request, response);
    }
}
