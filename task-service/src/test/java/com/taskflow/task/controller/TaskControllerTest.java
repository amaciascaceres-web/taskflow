package com.taskflow.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.task.application.TaskService;
import com.taskflow.task.config.SecurityConfig;
import com.taskflow.task.controller.dto.CreateTaskRequest;
import com.taskflow.task.controller.dto.TaskResponse;
import com.taskflow.task.controller.dto.UpdateTaskRequest;
import com.taskflow.task.domain.TaskPriority;
import com.taskflow.task.domain.TaskStatus;
import com.taskflow.task.domain.exception.AssigneeNotFoundException;
import com.taskflow.task.domain.exception.ServiceUnavailableException;
import com.taskflow.task.domain.exception.TaskNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@Import(SecurityConfig.class)
class TaskControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean TaskService taskService;

    private static final TaskResponse TASK_RESPONSE = new TaskResponse(
            1L, "Test task", "Description", TaskStatus.TODO, TaskPriority.HIGH,
            null, null, LocalDateTime.now(), LocalDateTime.now());

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
        when(taskService.getById(1L)).thenReturn(TASK_RESPONSE);

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test task"));
    }

    @Test
    void getById_notFound_returns404WithTaskNotFoundCode() throws Exception {
        when(taskService.getById(99L)).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/tasks/99"));
    }

    // ── GET /api/tasks ────────────────────────────────────────────────────────

    @Test
    void getAll_withValidStatusSlug_returns200() throws Exception {
        when(taskService.findAll(TaskStatus.TODO)).thenReturn(List.of(TASK_RESPONSE));

        mockMvc.perform(get("/api/tasks").param("status", "todo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getAll_withInvalidStatusSlug_returns500WithInternalErrorCode() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "not-a-status"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    // ── PUT /api/tasks/{id} ───────────────────────────────────────────────────

    @Test
    void update_found_returns200() throws Exception {
        when(taskService.update(eq(1L), any())).thenReturn(TASK_RESPONSE);

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_notFound_returns404WithTaskNotFoundCode() throws Exception {
        when(taskService.update(eq(99L), any())).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(put("/api/tasks/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
    }

    @Test
    void update_assigneeNotFound_returns404WithAssigneeNotFoundCode() throws Exception {
        when(taskService.update(eq(1L), any())).thenThrow(new AssigneeNotFoundException(99L));

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ASSIGNEE_NOT_FOUND"));
    }

    @Test
    void update_userServiceDown_returns503() throws Exception {
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
        doNothing().when(taskService).delete(1L);

        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());
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
