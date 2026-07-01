package com.taskflow.notification.application;

import com.taskflow.notification.infrastructure.TaskCreatedEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks NotificationService notificationService;

    @Test
    void handleTaskCreated_validEvent_doesNotThrow() {
        TaskCreatedEvent event = new TaskCreatedEvent(1L, "Fix bug", 2L, LocalDateTime.now());

        notificationService.handleTaskCreated(event);
    }

    @Test
    void handleTaskCreated_noAssignee_doesNotThrow() {
        TaskCreatedEvent event = new TaskCreatedEvent(1L, "Unassigned task", null, LocalDateTime.now());

        notificationService.handleTaskCreated(event);
    }
}
