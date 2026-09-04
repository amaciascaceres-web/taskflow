package com.taskflow.task.application;

import com.taskflow.task.config.AuthenticatedUser;
import com.taskflow.task.infrastructure.repository.TaskRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Domain-object authorization for @PreAuthorize (ADR-016): a user may act
 * on a task only if they are its assignee, checked here instead of inline
 * SpEL so the rule is independently testable without a Spring context.
 */
@Service("taskSecurity")
@RequiredArgsConstructor
public class TaskSecurityService {

    private final TaskRepository taskRepository;

    public boolean isOwner(Long taskId, Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return taskRepository.findById(taskId)
                .map(task -> principal.id().equals(task.getAssigneeId()))
                .orElse(false);
    }
}
