package com.taskflow.task.controller.dto;

import com.taskflow.task.domain.TaskPriority;
import com.taskflow.task.domain.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse (
        long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        Long assigneeId,
        LocalDate dueDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
