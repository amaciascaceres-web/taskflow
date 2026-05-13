package com.taskflow.task.infrastructure.client;

import java.time.LocalDateTime;

public record TaskCreatedEvent(
        Long taskId,
        String title,
        Long assigneeId,
        LocalDateTime occurredAt
) {}
