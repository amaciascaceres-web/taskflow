package com.taskflow.task.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.taskflow.task.controller.dto.CreateTaskRequest;
import com.taskflow.task.controller.dto.TaskResponse;
import com.taskflow.task.controller.dto.UpdateTaskRequest;
import com.taskflow.task.domain.TaskPriority;
import com.taskflow.task.domain.TaskStatus;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
        "services.user-service.url=http://localhost:9561",
        "services.notification-service.url=http://localhost:9562",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cache.type=none"
})
class TaskE2ETest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static WireMockServer userServiceMock;
    static WireMockServer notificationServiceMock;

    @Autowired TestRestTemplate restTemplate;
    @Autowired ObjectMapper objectMapper;

    @BeforeAll
    static void startWireMock() {
        userServiceMock = new WireMockServer(wireMockConfig().port(9561));
        notificationServiceMock = new WireMockServer(wireMockConfig().port(9562));
        userServiceMock.start();
        notificationServiceMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        userServiceMock.stop();
        notificationServiceMock.stop();
    }

    @BeforeEach
    void resetWireMock() {
        userServiceMock.resetAll();
        notificationServiceMock.resetAll();
        // Notification is fire-and-forget — stub a default success so it never blocks
        notificationServiceMock.stubFor(post(anyUrl()).willReturn(ok()));
    }

    // ── full CRUD flow ────────────────────────────────────────────────────────

    @Test
    void fullCrudFlow() {
        // POST
        ResponseEntity<TaskResponse> created = restTemplate.postForEntity(
                "/api/tasks", httpEntity(validCreateRequest()), TaskResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long id = created.getBody().id();
        assertThat(id).isNotNull();

        // GET
        ResponseEntity<TaskResponse> fetched = restTemplate.getForEntity("/api/tasks/" + id, TaskResponse.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().title()).isEqualTo("E2E Task");

        // PUT
        ResponseEntity<TaskResponse> updated = restTemplate.exchange(
                "/api/tasks/" + id, HttpMethod.PUT, httpEntity(validUpdateRequest()), TaskResponse.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().title()).isEqualTo("Updated E2E Task");

        // DELETE
        restTemplate.delete("/api/tasks/" + id);

        // GET after DELETE → 404
        ResponseEntity<String> gone = restTemplate.getForEntity("/api/tasks/" + id, String.class);
        assertThat(gone.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── status filter ─────────────────────────────────────────────────────────

    @Test
    void getAll_statusFilter_returnsOnlyMatchingTasks() {
        restTemplate.postForEntity("/api/tasks", httpEntity(createRequestWithStatus("todo")), TaskResponse.class);
        restTemplate.postForEntity("/api/tasks", httpEntity(createRequestWithStatus("in-progress")), TaskResponse.class);

        ResponseEntity<List> todos = restTemplate.getForEntity("/api/tasks?status=todo", List.class);

        assertThat(todos.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(todos.getBody()).isNotEmpty()
                .allMatch(t -> ((java.util.LinkedHashMap<?, ?>) t).get("status").toString().equals("todo"));
    }

    // ── assignee validation ───────────────────────────────────────────────────

    @Test
    void createTask_validAssignee_taskCreated() {
        userServiceMock.stubFor(get(urlEqualTo("/api/users/1"))
                .willReturn(ok().withBody("{}").withHeader("Content-Type", "application/json")));

        CreateTaskRequest request = validCreateRequest();
        request.setAssigneeId(1L);

        ResponseEntity<TaskResponse> response = restTemplate.postForEntity(
                "/api/tasks", httpEntity(request), TaskResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().assigneeId()).isEqualTo(1L);
    }

    @Test
    void createTask_assigneeNotFound_returns404() {
        userServiceMock.stubFor(get(urlEqualTo("/api/users/99")).willReturn(notFound()));

        CreateTaskRequest request = validCreateRequest();
        request.setAssigneeId(99L);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/tasks", httpEntity(request), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("ASSIGNEE_NOT_FOUND");
    }

    @Test
    void createTask_userServiceDown_returns503() {
        userServiceMock.stubFor(get(urlEqualTo("/api/users/1"))
                .willReturn(aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

        CreateTaskRequest request = validCreateRequest();
        request.setAssigneeId(1L);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/tasks", httpEntity(request), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).contains("SERVICE_UNAVAILABLE");
    }

    // ── fire-and-forget notification ──────────────────────────────────────────

    @Test
    void createTask_notificationServiceDown_taskStillCreated() {
        notificationServiceMock.resetAll();
        notificationServiceMock.stubFor(post(anyUrl())
                .willReturn(aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

        ResponseEntity<TaskResponse> response = restTemplate.postForEntity(
                "/api/tasks", httpEntity(validCreateRequest()), TaskResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().id()).isNotNull();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CreateTaskRequest validCreateRequest() {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("E2E Task");
        req.setPriority(TaskPriority.HIGH);
        req.setStatus("todo");
        return req;
    }

    private CreateTaskRequest createRequestWithStatus(String status) {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Task " + status);
        req.setPriority(TaskPriority.LOW);
        req.setStatus(status);
        return req;
    }

    private UpdateTaskRequest validUpdateRequest() {
        UpdateTaskRequest req = new UpdateTaskRequest();
        req.setTitle("Updated E2E Task");
        req.setPriority(TaskPriority.MEDIUM);
        req.setStatus(TaskStatus.IN_PROGRESS);
        return req;
    }

    private <T> HttpEntity<T> httpEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
