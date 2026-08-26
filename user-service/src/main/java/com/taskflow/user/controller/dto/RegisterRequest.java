package com.taskflow.user.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// No "role" field on purpose: role assignment above USER must never be
// reachable through an unauthenticated endpoint (ADR-015 follow-up).
@Getter @Setter @NoArgsConstructor
public class RegisterRequest {

    @JsonProperty("email")
    @NotBlank(message = "Email is required")
    @Size(max = 200, message = "Email must be less than 200 characters")
    private String email;

    @JsonProperty("password")
    @NotBlank(message = "Password is required")
    private String password;

    @JsonProperty("name")
    @Size(max = 200, message = "Name must be less than 200 characters")
    private String name;

    @JsonProperty("team")
    @Size(max = 200, message = "Team must be less than 200 characters")
    private String team;
}
