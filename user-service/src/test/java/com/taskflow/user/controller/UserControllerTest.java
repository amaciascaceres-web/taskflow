package com.taskflow.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.user.application.UserService;
import com.taskflow.user.controller.dto.UserRequest;
import com.taskflow.user.controller.dto.UserResponse;
import com.taskflow.user.domain.exception.UserNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean UserService userService;

    private static final UserResponse USER_RESPONSE = new UserResponse(
            1L, "Alice", "Backend", "alice@example.com",
            LocalDateTime.now(), LocalDateTime.now());

    // ── GET /health ───────────────────────────────────────────────────────────

    @Test
    void health_returns200() throws Exception {
        mockMvc.perform(get("/api/users/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // ── GET /api/users ────────────────────────────────────────────────────────

    @Test
    void getAll_returns200WithList() throws Exception {
        when(userService.getAll()).thenReturn(List.of(USER_RESPONSE));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    // ── GET /api/users/{id} ───────────────────────────────────────────────────

    @Test
    void getById_found_returns200() throws Exception {
        when(userService.getById(1L)).thenReturn(USER_RESPONSE);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(userService.getById(99L)).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    // ── PUT /api/users/{id} ───────────────────────────────────────────────────

    @Test
    void update_found_returns200() throws Exception {
        when(userService.update(eq(1L), any())).thenReturn(USER_RESPONSE);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        when(userService.update(eq(99L), any())).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(put("/api/users/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    // ── DELETE /api/users/{id} ────────────────────────────────────────────────

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(userService).delete(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private UserRequest validRequest() {
        UserRequest req = new UserRequest();
        req.setName("Alice");
        req.setEmail("alice@example.com");
        req.setTeam("Backend");
        return req;
    }
}
