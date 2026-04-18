package com.taskflow.user.controller.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record UserResponse(Long id, String name, String team, String email, LocalDateTime createdAt, LocalDateTime updatedAt) {}
