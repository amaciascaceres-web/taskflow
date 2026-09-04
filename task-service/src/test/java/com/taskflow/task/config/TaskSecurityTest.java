package com.taskflow.task.config;

import com.taskflow.task.application.TaskService;
import com.taskflow.task.controller.TaskController;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dedicated coverage for the header-trust boundary (ADR-015): task-service
 * builds its SecurityContext from X-User-Email/X-User-Role, never from a
 * token or a database lookup. Kept separate from TaskControllerTest so
 * business-logic tests don't need to know about auth headers.
 */
@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, HeaderAuthenticationFilter.class})
class TaskSecurityTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean TaskService taskService;

    @Test
    void request_withTrustedHeaders_isAuthenticated() throws Exception {
        mockMvc.perform(get("/api/tasks/ping")
                        .header("X-User-Id", "1")
                        .header("X-User-Email", "user@test.com")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isOk());
    }

    @Test
    void request_withoutHeaders_returns403() throws Exception {
        mockMvc.perform(get("/api/tasks/ping"))
                .andExpect(status().isForbidden());
    }

    @Test
    void request_withoutIdHeader_returns403() throws Exception {
        mockMvc.perform(get("/api/tasks/ping")
                        .header("X-User-Email", "user@test.com")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void request_withOnlyEmailHeader_returns403() throws Exception {
        mockMvc.perform(get("/api/tasks/ping")
                        .header("X-User-Email", "user@test.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    void request_withOnlyRoleHeader_returns403() throws Exception {
        mockMvc.perform(get("/api/tasks/ping")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void request_withMalformedIdHeader_returns403() throws Exception {
        mockMvc.perform(get("/api/tasks/ping")
                        .header("X-User-Id", "not-a-number")
                        .header("X-User-Email", "user@test.com")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }
}
