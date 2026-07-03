package com.taskflow.task.infrastructure.repository;

import com.taskflow.task.domain.TaskPriority;
import com.taskflow.task.domain.TaskStatus;
import com.taskflow.task.infrastructure.entity.TaskEntity;

import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class TaskSpecification {

    private TaskSpecification() {}

    public static Specification<TaskEntity> hasStatusIn(List<TaskStatus> statuses) {
        return (root, query, cb) ->
            (statuses == null || statuses.isEmpty()) ? null : root.get("status").in(statuses);
    }

    public static Specification<TaskEntity> hasAssigneeIdIn(List<Long> assigneeIds) {
        return (root, query, cb) ->
            (assigneeIds == null || assigneeIds.isEmpty()) ? null : root.get("assigneeId").in(assigneeIds);
    }

    public static Specification<TaskEntity> hasPriorityIn(List<TaskPriority> priorities) {
        return (root, query, cb) ->
            (priorities == null || priorities.isEmpty()) ? null : root.get("priority").in(priorities);
    }
}
