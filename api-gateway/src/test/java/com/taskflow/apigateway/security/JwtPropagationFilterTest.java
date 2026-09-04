package com.taskflow.apigateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the header-trust boundary (ADR-015): this filter is the only place
 * in the system that validates a JWT signature, and the only place that
 * strips client-supplied X-User-* headers before trusting its own. A bug
 * in the strip-then-set order here is an authentication bypass across
 * every internal service, not just api-gateway.
 */
class JwtPropagationFilterTest {

    private static final String SECRET = "test-secret-min-32-chars-long-ok!";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    private JwtPropagationFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        filter = new JwtPropagationFilter(jwtService);

        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void noAuthorizationHeader_forwardsRequestUnchanged() {
        ServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/tasks"));

        filter.filter(exchange, chain).block();

        ServerWebExchange forwarded = capturedExchange();
        assertThat(forwarded.getRequest().getHeaders().get(USER_ID_HEADER)).isNull();
        assertThat(forwarded.getRequest().getHeaders().get(USER_EMAIL_HEADER)).isNull();
        assertThat(forwarded.getRequest().getHeaders().get(USER_ROLE_HEADER)).isNull();
    }

    @Test
    void noAuthorizationHeader_stripsClientSuppliedTrustedHeaders() {
        ServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/tasks")
                .header(USER_ID_HEADER, "999")
                .header(USER_EMAIL_HEADER, "attacker@evil.com")
                .header(USER_ROLE_HEADER, "ADMIN"));

        filter.filter(exchange, chain).block();

        ServerWebExchange forwarded = capturedExchange();
        assertThat(forwarded.getRequest().getHeaders().get(USER_ID_HEADER)).isNull();
        assertThat(forwarded.getRequest().getHeaders().get(USER_EMAIL_HEADER)).isNull();
        assertThat(forwarded.getRequest().getHeaders().get(USER_ROLE_HEADER)).isNull();
    }

    @Test
    void validToken_injectsTrustedHeadersFromClaims() {
        String token = tokenFor(1L, "alice@example.com", "USER");
        ServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/tasks")
                .header("Authorization", "Bearer " + token));

        filter.filter(exchange, chain).block();

        ServerWebExchange forwarded = capturedExchange();
        assertThat(forwarded.getRequest().getHeaders().getFirst(USER_ID_HEADER)).isEqualTo("1");
        assertThat(forwarded.getRequest().getHeaders().getFirst(USER_EMAIL_HEADER)).isEqualTo("alice@example.com");
        assertThat(forwarded.getRequest().getHeaders().getFirst(USER_ROLE_HEADER)).isEqualTo("USER");
    }

    @Test
    void validToken_overridesClientSuppliedTrustedHeaders() {
        String token = tokenFor(1L, "alice@example.com", "USER");
        ServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/tasks")
                .header("Authorization", "Bearer " + token)
                .header(USER_ID_HEADER, "999")
                .header(USER_EMAIL_HEADER, "attacker@evil.com")
                .header(USER_ROLE_HEADER, "ADMIN"));

        filter.filter(exchange, chain).block();

        ServerWebExchange forwarded = capturedExchange();
        assertThat(forwarded.getRequest().getHeaders().get(USER_ID_HEADER)).containsExactly("1");
        assertThat(forwarded.getRequest().getHeaders().get(USER_EMAIL_HEADER)).containsExactly("alice@example.com");
        assertThat(forwarded.getRequest().getHeaders().get(USER_ROLE_HEADER)).containsExactly("USER");
    }

    @Test
    void invalidToken_returns401_andNeverCallsChain() {
        ServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/tasks")
                .header("Authorization", "Bearer not-a-valid-jwt"));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    private ServerWebExchange capturedExchange() {
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        return captor.getValue();
    }

    private static ServerWebExchange exchange(MockServerHttpRequest.BaseBuilder<?> requestBuilder) {
        return MockServerWebExchange.from(requestBuilder.build());
    }

    private static String tokenFor(Long id, String subject, String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .claim("id", String.valueOf(id))
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
    }
}
