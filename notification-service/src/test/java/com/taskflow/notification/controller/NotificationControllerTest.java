package com.taskflow.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.notification.application.NotificationService;
import com.taskflow.notification.infrastructure.TaskCreatedEvent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean NotificationService notificationService;

    // ── POST /internal/notifications/task-created ─────────────────────────────

    @Test
    void taskCreated_validEvent_returns200AndDelegatesToService() throws Exception {
        doNothing().when(notificationService).handleTaskCreated(any());

        mockMvc.perform(post("/internal/notifications/task-created")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEvent())))
                .andExpect(status().isOk());

        verify(notificationService).handleTaskCreated(any());
    }

    @Test
    void taskCreated_missingTaskId_returns400() throws Exception {
        TaskCreatedEvent event = new TaskCreatedEvent(null, "Fix bug", 1L, LocalDateTime.now());

        mockMvc.perform(post("/internal/notifications/task-created")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest());

        verify(notificationService, never()).handleTaskCreated(any());
    }

    @Test
    void taskCreated_missingTitle_returns400() throws Exception {
        TaskCreatedEvent event = new TaskCreatedEvent(1L, null, 1L, LocalDateTime.now());

        mockMvc.perform(post("/internal/notifications/task-created")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest());

        verify(notificationService, never()).handleTaskCreated(any());
    }

    @Test
    void taskCreated_noAssignee_returns200() throws Exception {
        TaskCreatedEvent event = new TaskCreatedEvent(1L, "Fix bug", null, LocalDateTime.now());

        mockMvc.perform(post("/internal/notifications/task-created")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private TaskCreatedEvent validEvent() {
        return new TaskCreatedEvent(1L, "Fix bug", 2L, LocalDateTime.now());
    }
}
