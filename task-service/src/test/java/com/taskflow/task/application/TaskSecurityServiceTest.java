package com.taskflow.task.application;

import com.taskflow.task.config.AuthenticatedUser;
import com.taskflow.task.infrastructure.entity.TaskEntity;
import com.taskflow.task.infrastructure.repository.TaskRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskSecurityServiceTest {

    @Mock TaskRepository taskRepository;

    private TaskSecurityService taskSecurityService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        taskSecurityService = new TaskSecurityService(taskRepository);
    }

    @Test
    void isOwner_matchingAssigneeId_returnsTrue() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(taskAssignedTo(7L)));

        assertThat(taskSecurityService.isOwner(1L, authenticatedAs(7L))).isTrue();
    }

    @Test
    void isOwner_differentAssigneeId_returnsFalse() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(taskAssignedTo(999L)));

        assertThat(taskSecurityService.isOwner(1L, authenticatedAs(7L))).isFalse();
    }

    @Test
    void isOwner_taskWithNoAssignee_returnsFalse() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(taskAssignedTo(null)));

        assertThat(taskSecurityService.isOwner(1L, authenticatedAs(7L))).isFalse();
    }

    @Test
    void isOwner_taskNotFound_returnsFalse() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(taskSecurityService.isOwner(99L, authenticatedAs(7L))).isFalse();
    }

    private TaskEntity taskAssignedTo(Long assigneeId) {
        return TaskEntity.builder().id(1L).assigneeId(assigneeId).build();
    }

    private Authentication authenticatedAs(Long id) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(id, "user" + id + "@test.com"), null);
    }
}
