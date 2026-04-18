package com.taskflow.user.application.mapper;

import com.taskflow.user.controller.dto.UserRequest;
import com.taskflow.user.controller.dto.UserResponse;
import com.taskflow.user.infrastructure.entity.UserEntity;

import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserEntity toEntity(UserRequest request) {
        return UserEntity.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();
    }

    public UserResponse toResponse(UserEntity entity) {
        return UserResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
