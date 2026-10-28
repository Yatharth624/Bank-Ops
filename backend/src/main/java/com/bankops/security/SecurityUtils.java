package com.bankops.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return Optional.of((UserPrincipal) authentication.getPrincipal());
        }
        return Optional.empty();
    }

    public static String getCurrentUsername() {
        return getCurrentUserPrincipal()
                .map(UserPrincipal::getUsername)
                .orElse("SYSTEM");
    }

    public static UUID getCurrentUserId() {
        return getCurrentUserPrincipal()
                .map(UserPrincipal::getId)
                .orElse(null);
    }

    public static String getCurrentUserPrimaryRole() {
        return getCurrentUserPrincipal()
                .flatMap(u -> u.getAuthorities().stream().findFirst().map(GrantedAuthority::getAuthority))
                .orElse("ROLE_ANONYMOUS");
    }

    public static boolean hasRole(String role) {
        return getCurrentUserPrincipal()
                .map(u -> u.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role)))
                .orElse(false);
    }
}
