package com.worksure.security;

import com.worksure.web.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static AuthUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            throw new ApiException(401, "Authentication required");
        }
        return user;
    }

    public static AuthUser currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            return null;
        }
        return user;
    }

    public static void requireRole(AuthUser user, String... roles) {
        if (user == null) {
            throw new ApiException(401, "Authentication required");
        }
        for (String role : roles) {
            if (role.equals(user.role())) {
                return;
            }
        }
        throw new ApiException(403, "Forbidden");
    }
}
