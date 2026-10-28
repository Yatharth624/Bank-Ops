package com.bankops.security;

import com.bankops.modules.user.Role;
import com.bankops.modules.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(TEST_SECRET, 3600000L, "bankops-test");
    }

    @Test
    @DisplayName("Should generate valid JWT token containing username claim")
    void shouldGenerateAndValidateToken() {
        Role role = Role.builder().name("ROLE_ANALYST").build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("analyst_david")
                .fullName("David Chen")
                .email("david@bankops.internal")
                .active(true)
                .roles(Set.of(role))
                .build();

        UserPrincipal principal = UserPrincipal.create(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        String token = tokenProvider.generateToken(auth);

        assertThat(token).isNotBlank();
        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo("analyst_david");
    }

    @Test
    @DisplayName("Should reject malformed or tampered token")
    void shouldRejectMalformedToken() {
        assertThat(tokenProvider.validateToken("invalid.jwt.token")).isFalse();
    }
}
