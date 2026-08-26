package com.taskflow.user.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.user.controller.dto.LoginRequest;
import com.taskflow.user.controller.dto.RegisterRequest;
import com.taskflow.user.controller.dto.TokenResponse;
import com.taskflow.user.controller.dto.UserRequest;
import com.taskflow.user.controller.dto.UserResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class UserE2ETest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired TestRestTemplate restTemplate;
    @Autowired ObjectMapper objectMapper;

    // ── register + profile CRUD flow ────────────────────────────────────────────

    @Test
    void fullCrudFlow() {
        // POST /auth/register
        ResponseEntity<UserResponse> created = restTemplate.postForEntity(
                "/auth/register", httpEntity(registerRequest("Alice", "alice-crud@example.com")), UserResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long id = created.getBody().id();
        assertThat(id).isNotNull();
        assertThat(created.getBody().team()).isEqualTo("Backend");

        // GET by id
        ResponseEntity<UserResponse> fetched = restTemplate.getForEntity("/api/users/" + id, UserResponse.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().name()).isEqualTo("Alice");

        // PUT
        ResponseEntity<UserResponse> updated = restTemplate.exchange(
                "/api/users/" + id, HttpMethod.PUT,
                httpEntity(profileUpdateRequest("Alice Updated", "alice-crud@example.com")), UserResponse.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().name()).isEqualTo("Alice Updated");

        // DELETE
        restTemplate.delete("/api/users/" + id);

        // GET after DELETE → 404
        ResponseEntity<String> gone = restTemplate.getForEntity("/api/users/" + id, String.class);
        assertThat(gone.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(gone.getBody()).contains("USER_NOT_FOUND");
    }

    // ── duplicate email ───────────────────────────────────────────────────────

    @Test
    void register_duplicateEmail_returns409() {
        restTemplate.postForEntity("/auth/register",
                httpEntity(registerRequest("Alice", "alice-dup@example.com")), UserResponse.class);

        ResponseEntity<String> response = restTemplate.postForEntity("/auth/register",
                httpEntity(registerRequest("Alice2", "alice-dup@example.com")), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("USER_ALREADY_EXISTS");
    }

    // ── validation ────────────────────────────────────────────────────────────

    @Test
    void register_missingEmail_returns400() {
        RegisterRequest req = new RegisterRequest();
        req.setPassword("s3cret!");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/register", httpEntity(req), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("VALIDATION_ERROR");
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsToken() {
        restTemplate.postForEntity("/auth/register",
                httpEntity(registerRequest("Carol", "carol@example.com")), UserResponse.class);

        LoginRequest login = new LoginRequest();
        login.setEmail("carol@example.com");
        login.setPassword("s3cret!");

        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                "/auth/login", httpEntity(login), TokenResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().token()).isNotBlank();
    }

    @Test
    void login_wrongPassword_returns401() {
        restTemplate.postForEntity("/auth/register",
                httpEntity(registerRequest("Dave", "dave@example.com")), UserResponse.class);

        LoginRequest login = new LoginRequest();
        login.setEmail("dave@example.com");
        login.setPassword("wrong-password");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/login", httpEntity(login), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).contains("INVALID_CREDENTIALS");
    }

    // ── GET all ───────────────────────────────────────────────────────────────

    @Test
    void getAll_returnsCreatedUsers() {
        restTemplate.postForEntity("/auth/register",
                httpEntity(registerRequest("Bob", "bob@example.com")), UserResponse.class);

        ResponseEntity<List> response = restTemplate.getForEntity("/api/users", List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private RegisterRequest registerRequest(String name, String email) {
        RegisterRequest req = new RegisterRequest();
        req.setName(name);
        req.setEmail(email);
        req.setPassword("s3cret!");
        req.setTeam("Backend");
        return req;
    }

    private UserRequest profileUpdateRequest(String name, String email) {
        UserRequest req = new UserRequest();
        req.setName(name);
        req.setEmail(email);
        req.setTeam("Backend");
        return req;
    }

    private <T> HttpEntity<T> httpEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
