package com.taskflow.notification.infrastructure;

import java.time.LocalDateTime;

public record TaskCreatedEvent(
        Long taskId,
        String title,
        Long assigneeId,
        LocalDateTime occurredAt
) {}
