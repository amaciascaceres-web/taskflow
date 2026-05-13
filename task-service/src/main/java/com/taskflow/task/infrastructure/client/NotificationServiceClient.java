package com.taskflow.task.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.notification-service.url:http://localhost:8082}")
    private String notificationServiceUrl;

    public void notifyTaskCreated(Long taskId, String title, Long assigneeId) {
        try {
            TaskCreatedEvent event = new TaskCreatedEvent(
                    taskId, title, assigneeId, LocalDateTime.now());
            restTemplate.postForObject(
                    notificationServiceUrl + "/internal/notifications/task-created",
                    event, Void.class);
            log.debug("Notification sent for taskId={}", taskId);
        } catch (Exception e) {
            // Fire and forget: log the error but do not propagate it
            log.warn("Failed to send notification for taskId={}: {}",
                    taskId, e.getMessage());
        }
    }
}
