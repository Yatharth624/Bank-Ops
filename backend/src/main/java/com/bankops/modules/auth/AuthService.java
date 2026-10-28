package com.bankops.modules.auth;

import com.bankops.modules.audit.AuditService;
import com.bankops.security.JwtTokenProvider;
import com.bankops.security.SecurityUtils;
import com.bankops.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;

    public AuthResponse login(LoginRequest request) {
        String username = request.getUsername().trim();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword()));

            String jwt = tokenProvider.generateToken(authentication);
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

            List<String> roles = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            // Record audit event without exposing credentials or JWT
            auditService.logEvent("LOGIN", "AUTHENTICATION", principal.getId().toString(),
                    String.format("User '%s' authenticated successfully", username), "SUCCESS");

            return AuthResponse.builder()
                    .token(jwt)
                    .tokenType("Bearer")
                    .userId(principal.getId())
                    .username(principal.getUsername())
                    .fullName(principal.getFullName())
                    .email(principal.getEmail())
                    .roles(roles)
                    .build();

        } catch (BadCredentialsException ex) {
            log.warn("Failed login attempt for username: '{}'", username);
            auditService.logEvent("LOGIN_FAILURE", "AUTHENTICATION", "UNKNOWN",
                    String.format("Failed login attempt for user '%s' (Invalid Credentials)", username), "FAILED");
            throw ex;
        }
    }

    public AuthResponse getCurrentUser() {
        UserPrincipal principal = SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new BadCredentialsException("Not authenticated"));

        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return AuthResponse.builder()
                .userId(principal.getId())
                .username(principal.getUsername())
                .fullName(principal.getFullName())
                .email(principal.getEmail())
                .roles(roles)
                .build();
    }
}
