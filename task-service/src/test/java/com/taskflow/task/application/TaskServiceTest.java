package com.taskflow.task.application;

import com.taskflow.task.application.mapper.TaskMapper;
import com.taskflow.task.controller.dto.CreateTaskRequest;
import com.taskflow.task.controller.dto.TaskResponse;
import com.taskflow.task.controller.dto.UpdateTaskRequest;
import com.taskflow.task.domain.TaskPriority;
import com.taskflow.task.domain.TaskStatus;
import com.taskflow.task.domain.exception.AssigneeNotFoundException;
import com.taskflow.task.domain.exception.ServiceUnavailableException;
import com.taskflow.task.domain.exception.TaskNotFoundException;
import com.taskflow.task.infrastructure.client.NotificationServiceClient;
import com.taskflow.task.infrastructure.client.UserServiceClient;
import com.taskflow.task.infrastructure.entity.TaskEntity;
import com.taskflow.task.infrastructure.repository.TaskRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock TaskMapper taskMapper;
    @Mock UserServiceClient userServiceClient;
    @Mock NotificationServiceClient notificationClient;

    @InjectMocks TaskService taskService;

    private TaskEntity savedEntity;
    private TaskResponse expectedResponse;

    @BeforeEach
    void setUp() {
        savedEntity = TaskEntity.builder()
                .id(1L)
                .title("Test task")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        expectedResponse = new TaskResponse(
                1L, "Test task", null, TaskStatus.TODO, TaskPriority.HIGH,
                null, null, savedEntity.getCreatedAt(), savedEntity.getUpdatedAt());
    }

    // ── createTask ────────────────────────────────────────────────────────────

    @Test
    void createTask_happyPath_noAssignee() {
        CreateTaskRequest request = buildCreateRequest("Test task", TaskPriority.HIGH, "todo", null);
        when(taskMapper.toEntity(request)).thenReturn(savedEntity);
        when(taskRepository.save(savedEntity)).thenReturn(savedEntity);
        when(taskMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        TaskResponse result = taskService.createTask(request);

        assertThat(result).isEqualTo(expectedResponse);
        verify(taskRepository).save(savedEntity);
        verify(notificationClient).notifyTaskCreated(1L, "Test task", null);
        verifyNoInteractions(userServiceClient);
    }

    @Test
    void createTask_nullStatus_defaultsToTodo() {
        CreateTaskRequest request = buildCreateRequest("Task", TaskPriority.HIGH, null, null);
        when(taskMapper.toEntity(request)).thenReturn(savedEntity);
        when(taskRepository.save(any())).thenReturn(savedEntity);
        when(taskMapper.toResponse(any())).thenReturn(expectedResponse);

        taskService.createTask(request);

        assertThat(request.getStatus()).isEqualTo("todo");
    }

    @Test
    void createTask_nullPriority_defaultsToLow() {
        CreateTaskRequest request = buildCreateRequest("Task", null, "todo", null);
        when(taskMapper.toEntity(request)).thenReturn(savedEntity);
        when(taskRepository.save(any())).thenReturn(savedEntity);
        when(taskMapper.toResponse(any())).thenReturn(expectedResponse);

        taskService.createTask(request);

        assertThat(request.getPriority()).isEqualTo(TaskPriority.LOW);
    }

    @Test
    void createTask_withExistingAssignee_saveSucceeds() {
        CreateTaskRequest request = buildCreateRequest("Task", TaskPriority.HIGH, "todo", 42L);
        when(userServiceClient.userExists(42L)).thenReturn(true);
        when(taskMapper.toEntity(request)).thenReturn(savedEntity);
        when(taskRepository.save(any())).thenReturn(savedEntity);
        when(taskMapper.toResponse(any())).thenReturn(expectedResponse);

        TaskResponse result = taskService.createTask(request);

        assertThat(result).isEqualTo(expectedResponse);
        verify(userServiceClient).userExists(42L);
    }

    @Test
    void createTask_assigneeNotFound_throwsAndDoesNotSave() {
        CreateTaskRequest request = buildCreateRequest("Task", TaskPriority.HIGH, "todo", 99L);
        when(userServiceClient.userExists(99L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(AssigneeNotFoundException.class);

        verifyNoInteractions(taskRepository);
    }

    @Test
    void createTask_userServiceUnavailable_throwsAndDoesNotSave() {
        CreateTaskRequest request = buildCreateRequest("Task", TaskPriority.HIGH, "todo", 1L);
        when(userServiceClient.userExists(1L)).thenThrow(new ServiceUnavailableException("user-service"));

        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(ServiceUnavailableException.class);

        verifyNoInteractions(taskRepository);
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_found_returnsResponse() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(savedEntity));
        when(taskMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        assertThat(taskService.getById(1L)).isEqualTo(expectedResponse);
    }

    @Test
    void getById_notFound_throwsTaskNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getById(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void findAll_allNullFilters_returnsPageOfAllTasks() {
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(savedEntity)));
        when(taskMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        Page<TaskResponse> result = taskService.findAll(null, null, null, Pageable.unpaged());

        assertThat(result.getContent()).containsExactly(expectedResponse);
        verify(taskRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_withMultipleStatuses_usesSpecification() {
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(savedEntity)));
        when(taskMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        Page<TaskResponse> result = taskService.findAll(
                List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS), null, null, Pageable.unpaged());

        assertThat(result.getContent()).containsExactly(expectedResponse);
        verify(taskRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_withAllFilters_usesSpecification() {
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(savedEntity)));
        when(taskMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        Page<TaskResponse> result = taskService.findAll(
                List.of(TaskStatus.TODO), List.of(1L), List.of(TaskPriority.HIGH), Pageable.unpaged());

        assertThat(result.getContent()).containsExactly(expectedResponse);
        verify(taskRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_withPageable_respectsPageSize() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 5);
        when(taskRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(savedEntity), pageable, 1L));
        when(taskMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        Page<TaskResponse> result = taskService.findAll(null, null, null, pageable);

        assertThat(result.getSize()).isEqualTo(5);
        verify(taskRepository).findAll(any(Specification.class), eq(pageable));
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_found_updatesAllFieldsAndReturns() {
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Updated");
        request.setDescription("New description");
        request.setStatus(TaskStatus.IN_PROGRESS);
        request.setPriority(TaskPriority.LOW);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(savedEntity));
        when(taskRepository.save(savedEntity)).thenReturn(savedEntity);
        when(taskMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        taskService.update(1L, request);

        assertThat(savedEntity.getTitle()).isEqualTo("Updated");
        assertThat(savedEntity.getDescription()).isEqualTo("New description");
        assertThat(savedEntity.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(savedEntity.getPriority()).isEqualTo(TaskPriority.LOW);
    }

    @Test
    void update_notFound_throwsTaskNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(99L, new UpdateTaskRequest()))
                .isInstanceOf(TaskNotFoundException.class);

        verify(taskRepository, never()).save(any());
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_found_deletesEntity() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(savedEntity));

        taskService.delete(1L);

        verify(taskRepository).delete(savedEntity);
    }

    @Test
    void delete_notFound_throwsTaskNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(99L))
                .isInstanceOf(TaskNotFoundException.class);

        verify(taskRepository, never()).delete(any(TaskEntity.class));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CreateTaskRequest buildCreateRequest(String title, TaskPriority priority,
                                                  String status, Long assigneeId) {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle(title);
        req.setPriority(priority);
        req.setStatus(status);
        req.setAssigneeId(assigneeId);
        return req;
    }
}
