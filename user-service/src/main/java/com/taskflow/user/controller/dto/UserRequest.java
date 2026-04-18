package com.taskflow.user.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class UserRequest {

    @JsonProperty("name")
    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must be less than 200 characters")
    private String name;

    @JsonProperty("email")
    @NotBlank(message = "Email is required")
    @Size(max = 200, message = "Email must be less than 200 characters")
    private String email;

    @JsonProperty("team")
    @Size(max = 200, message = "Team must be less than 200 characters")
    private String team;
}
