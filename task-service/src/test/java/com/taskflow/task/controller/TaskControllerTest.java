package com.taskflow.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.task.application.TaskSecurityService;
import com.taskflow.task.application.TaskService;
import com.taskflow.task.config.AuthenticatedUser;
import com.taskflow.task.config.HeaderAuthenticationFilter;
import com.taskflow.task.config.SecurityConfig;
import com.taskflow.task.controller.dto.CreateTaskRequest;
import com.taskflow.task.controller.dto.TaskResponse;
import com.taskflow.task.controller.dto.UpdateTaskRequest;
import com.taskflow.task.domain.TaskPriority;
import com.taskflow.task.domain.TaskStatus;
import com.taskflow.task.domain.exception.AssigneeNotFoundException;
import com.taskflow.task.domain.exception.ServiceUnavailableException;
import com.taskflow.task.domain.exception.TaskNotFoundException;
import com.taskflow.task.infrastructure.entity.TaskEntity;
import com.taskflow.task.infrastructure.repository.TaskRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
// SecurityConfig carries @EnableMethodSecurity — without it, @PreAuthorize is
// silently inert in a @WebMvcTest slice and every call falls straight through
// to the controller regardless of who's "authenticated".
@Import({SecurityConfig.class, HeaderAuthenticationFilter.class, TaskSecurityService.class})
class TaskControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean TaskService taskService;
    @MockitoBean TaskRepository taskRepository;

    private static final TaskResponse TASK_RESPONSE = new TaskResponse(
            1L, "Test task", "Description", TaskStatus.TODO, TaskPriority.HIGH,
            null, null, LocalDateTime.now(), LocalDateTime.now());

    // With addFilters=false there's no servlet filter to carry a MockMvc
    // RequestPostProcessor's Authentication into the real SecurityContextHolder
    // (that transfer normally happens in a Spring Security filter, which is
    // exactly what's disabled here). MockMvc runs on the same thread as the
    // test though, so setting it directly is visible to @PreAuthorize's AOP
    // interceptor when it runs — no filter needed.
    private static void authenticateAsAdmin() {
        authenticateAs(1L, "admin@test.com", "ADMIN");
    }

    private static void authenticateAsUser(Long id) {
        authenticateAs(id, "user" + id + "@test.com", "USER");
    }

    private static void authenticateAs(Long id, String email, String role) {
        var principal = new AuthenticatedUser(id, email);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── ping ──────────────────────────────────────────────────────────────────

    @Test
    void ping_returns200() throws Exception {
        mockMvc.perform(get("/api/tasks/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("task-service is alive"));
    }

    // ── POST /api/tasks ───────────────────────────────────────────────────────

    @Test
    void createTask_validBody_returns201WithLocation() throws Exception {
        authenticateAsAdmin();
        when(taskService.createTask(any())).thenReturn(TASK_RESPONSE);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/tasks/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test task"));
    }

    @Test
    void createTask_missingTitle_returns400WithValidationError() throws Exception {
        authenticateAsAdmin();
        CreateTaskRequest request = validCreateRequest();
        request.setTitle(null);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    void createTask_pastDueDate_returns400() throws Exception {
        authenticateAsAdmin();
        CreateTaskRequest request = validCreateRequest();
        request.setDueDate(LocalDate.now().minusDays(1));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── GET /api/tasks/{id} ───────────────────────────────────────────────────

    @Test
    void getById_found_returns200() throws Exception {
        authenticateAsAdmin();
        when(taskService.getById(1L)).thenReturn(TASK_RESPONSE);

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test task"));
    }

    @Test
    void getById_notFound_returns404WithTaskNotFoundCode() throws Exception {
        authenticateAsAdmin();
        when(taskService.getById(99L)).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/tasks/99"));
    }

    // ── GET /api/tasks ────────────────────────────────────────────────────────

    @Test
    void getAll_noFilters_returns200WithPagedResponse() throws Exception {
        authenticateAsAdmin();
        when(taskService.findAll(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(TASK_RESPONSE)));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void getAll_withSingleStatusSlug_passesStatusListToService() throws Exception {
        authenticateAsAdmin();
        when(taskService.findAll(eq(List.of(TaskStatus.TODO)), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(TASK_RESPONSE)));

        mockMvc.perform(get("/api/tasks").param("status", "todo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void getAll_withMultipleStatusSlugs_passesStatusListToService() throws Exception {
        authenticateAsAdmin();
        when(taskService.findAll(
                eq(List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS)),
                isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(TASK_RESPONSE)));

        mockMvc.perform(get("/api/tasks").param("status", "todo").param("status", "in-progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void getAll_withAssigneeIds_passesAssigneeIdListToService() throws Exception {
        authenticateAsAdmin();
        when(taskService.findAll(isNull(), eq(List.of(1L, 2L)), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(TASK_RESPONSE)));

        mockMvc.perform(get("/api/tasks").param("assigneeId", "1").param("assigneeId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void getAll_withPrioritySlugs_passesPriorityListToService() throws Exception {
        authenticateAsAdmin();
        when(taskService.findAll(isNull(), isNull(), eq(List.of(TaskPriority.HIGH, TaskPriority.MEDIUM)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(TASK_RESPONSE)));

        mockMvc.perform(get("/api/tasks").param("priority", "high").param("priority", "medium"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void getAll_withAllFilters_passesAllListsToService() throws Exception {
        authenticateAsAdmin();
        when(taskService.findAll(
                eq(List.of(TaskStatus.TODO)), eq(List.of(1L)), eq(List.of(TaskPriority.HIGH)),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(TASK_RESPONSE)));

        mockMvc.perform(get("/api/tasks")
                        .param("status", "todo")
                        .param("assigneeId", "1")
                        .param("priority", "high"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void getAll_withInvalidStatusSlug_returns500WithInternalErrorCode() throws Exception {
        authenticateAsAdmin();
        mockMvc.perform(get("/api/tasks").param("status", "not-a-status"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void getAll_withInvalidPrioritySlug_returns500WithInternalErrorCode() throws Exception {
        authenticateAsAdmin();
        mockMvc.perform(get("/api/tasks").param("priority", "not-a-priority"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    // ── PUT /api/tasks/{id} ───────────────────────────────────────────────────

    @Test
    void update_found_returns200() throws Exception {
        authenticateAsAdmin();
        when(taskService.update(eq(1L), any())).thenReturn(TASK_RESPONSE);

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_notFound_returns404WithTaskNotFoundCode() throws Exception {
        authenticateAsAdmin();
        when(taskService.update(eq(99L), any())).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(put("/api/tasks/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
    }

    @Test
    void update_assigneeNotFound_returns404WithAssigneeNotFoundCode() throws Exception {
        authenticateAsAdmin();
        when(taskService.update(eq(1L), any())).thenThrow(new AssigneeNotFoundException(99L));

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ASSIGNEE_NOT_FOUND"));
    }

    @Test
    void update_userServiceDown_returns503() throws Exception {
        authenticateAsAdmin();
        when(taskService.update(eq(1L), any())).thenThrow(new ServiceUnavailableException("user-service"));

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));
    }

    // ── DELETE /api/tasks/{id} ────────────────────────────────────────────────

    @Test
    void delete_returns204() throws Exception {
        authenticateAsAdmin();
        doNothing().when(taskService).delete(1L);

        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());
    }

    // ── RBAC (ADR-016) ───────────────────────────────────────────────────────

    @Test
    void getAll_nonAdmin_returns403() throws Exception {
        authenticateAsUser(7L);

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(taskService);
    }

    @Test
    void getById_owner_returns200() throws Exception {
        authenticateAsUser(7L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(taskAssignedTo(7L)));
        when(taskService.getById(1L)).thenReturn(TASK_RESPONSE);

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_notOwnerNotAdmin_returns403() throws Exception {
        authenticateAsUser(7L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(taskAssignedTo(999L)));

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isForbidden());

        verify(taskService, never()).getById(any());
    }

    @Test
    void getById_taskDoesNotExist_nonAdmin_returns403NotFalling404() throws Exception {
        authenticateAsUser(7L);
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_authenticatedNonAdmin_returns201() throws Exception {
        authenticateAsUser(7L);
        when(taskService.createTask(any())).thenReturn(TASK_RESPONSE);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated());
    }

    private TaskEntity taskAssignedTo(Long assigneeId) {
        return TaskEntity.builder().id(1L).assigneeId(assigneeId).build();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CreateTaskRequest validCreateRequest() {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Test task");
        req.setPriority(TaskPriority.HIGH);
        req.setStatus("todo");
        return req;
    }

    private UpdateTaskRequest validUpdateRequest() {
        UpdateTaskRequest req = new UpdateTaskRequest();
        req.setTitle("Updated task");
        req.setPriority(TaskPriority.HIGH);
        req.setStatus(TaskStatus.TODO);
        return req;
    }
}
