package com.taskflow.task.application.mapper;

import com.taskflow.task.controller.dto.CreateTaskRequest;
import com.taskflow.task.controller.dto.TaskResponse;
import com.taskflow.task.domain.TaskStatus;
import com.taskflow.task.infrastructure.entity.TaskEntity;

import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public TaskEntity toEntity(CreateTaskRequest request) {
        return TaskEntity.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .status(TaskStatus.fromSlug(request.getStatus()))
                .assigneeId(request.getAssigneeId())
                .dueDate(request.getDueDate())
                .build();
    }

    public TaskResponse toResponse(TaskEntity entity) {
        return new TaskResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getPriority(),
                entity.getAssigneeId(),
                entity.getDueDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
