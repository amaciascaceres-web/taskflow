package com.taskflow.notification.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.notification.infrastructure.TaskCreatedEvent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class NotificationE2ETest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired ObjectMapper objectMapper;

    @Test
    void taskCreated_validEvent_returns200() {
        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/internal/notifications/task-created",
                httpEntity(validEvent()),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void taskCreated_noAssignee_returns200() {
        TaskCreatedEvent event = new TaskCreatedEvent(1L, "Fix bug", null, LocalDateTime.now());

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/internal/notifications/task-created",
                httpEntity(event),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void taskCreated_missingTaskId_returns400() {
        TaskCreatedEvent event = new TaskCreatedEvent(null, "Fix bug", 1L, LocalDateTime.now());

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/internal/notifications/task-created",
                httpEntity(event),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private TaskCreatedEvent validEvent() {
        return new TaskCreatedEvent(1L, "Fix bug", 2L, LocalDateTime.now());
    }

    private <T> HttpEntity<T> httpEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
