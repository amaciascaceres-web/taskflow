package com.taskflow.notification.infrastructure;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TaskCreatedEvent(
        @NotNull Long taskId,
        @NotBlank String title,
        Long assigneeId,
        LocalDateTime occurredAt
) {}
