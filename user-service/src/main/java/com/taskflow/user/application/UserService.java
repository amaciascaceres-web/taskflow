package com.taskflow.user.application;

import com.taskflow.user.controller.dto.UserRequest;
import com.taskflow.user.controller.dto.UserResponse;
import com.taskflow.user.domain.UserNotFoundException;
import com.taskflow.user.exception.UserAlreadyExistsException;
import com.taskflow.user.infrastructure.entity.UserEntity;
import com.taskflow.user.infrastructure.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse create(UserRequest request) {
        log.info("Creating user with name='{}', email='{}', team='{}'",
                request.getName(),
                request.getEmail(),
                request.getTeam());

        UserEntity user = UserEntity.builder()
                .name(request.getName())
                .email(request.getEmail())
                .team(request.getTeam())
                .build();

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        UserEntity savedUser = userRepository.save(user);

        log.info("Created user with id={}", savedUser.getId());

        return toResponse(savedUser);
    }

    public List<UserResponse> getAll() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse getById(Long id) {
        return toResponse(findUser(id));
    }

    public UserResponse update(Long id, UserRequest request) {
        log.info("Updating user id={} with name='{}', email='{}', team='{}'",
                id,
                request.getName(),
                request.getEmail(),
                request.getTeam());

        UserEntity user = findUser(id);

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setTeam(request.getTeam());

        UserEntity updatedUser = userRepository.save(user);

        log.info("Updated user id={}", updatedUser.getId());

        return toResponse(updatedUser);
    }

    public void delete(Long id) {
        log.info("Deleting user id={}", id);

        UserEntity user = findUser(id);
        userRepository.delete(user);

        log.info("Deleted user id={}", id);
    }

    private UserEntity findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private UserResponse toResponse(UserEntity user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .team(user.getTeam())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}