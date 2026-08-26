package com.taskflow.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-min-32-chars-long-ok!";

    private final JwtService jwtService = new JwtService();

    @BeforeEach
    void setSecret() {
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
    }

    @Test
    void validateAndExtractClaims_validToken_returnsClaims() {
        String token = tokenSignedWith(SECRET, "alice@example.com", "USER", 60_000);

        Claims claims = jwtService.validateAndExtractClaims(token);

        assertThat(claims.getSubject()).isEqualTo("alice@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    void validateAndExtractClaims_wrongSigningKey_throwsJwtException() {
        String token = tokenSignedWith("a-completely-different-secret-32b!", "alice@example.com", "USER", 60_000);

        assertThatThrownBy(() -> jwtService.validateAndExtractClaims(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateAndExtractClaims_expiredToken_throwsJwtException() {
        String token = tokenSignedWith(SECRET, "alice@example.com", "USER", -1_000);

        assertThatThrownBy(() -> jwtService.validateAndExtractClaims(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateAndExtractClaims_malformedToken_throwsJwtException() {
        assertThatThrownBy(() -> jwtService.validateAndExtractClaims("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }

    private static String tokenSignedWith(String secret, String subject, String role, long expiresInMs) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiresInMs))
                .signWith(key)
                .compact();
    }
}
