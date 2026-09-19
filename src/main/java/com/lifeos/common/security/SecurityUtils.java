package com.lifeos.common.security;

import com.lifeos.common.exception.UnauthorizedAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedAccessException("Current user is not authenticated");
        }
        return principal;
    }

    public static UUID getCurrentUserId() {
        return getCurrentUserPrincipal().getId();
    }

    public static String getCurrentUserEmail() {
        return getCurrentUserPrincipal().getEmail();
    }
}
