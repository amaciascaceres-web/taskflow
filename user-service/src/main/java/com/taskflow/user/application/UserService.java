package com.taskflow.user.application;

import com.taskflow.user.application.mapper.UserMapper;
import com.taskflow.user.controller.dto.UserRequest;
import com.taskflow.user.controller.dto.UserResponse;
import com.taskflow.user.domain.exception.UserNotFoundException;
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
    private final UserMapper userMapper;

    public List<UserResponse> getAll() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    public UserResponse getById(Long id) {
        return userMapper.toResponse(findUser(id));
    }

    public UserResponse update(Long id, UserRequest request) {
        log.info("Updating user id={} with name='{}', email='{}', team='{}'",
                id, request.getName(), request.getEmail(), request.getTeam());

        UserEntity user = findUser(id);
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setTeam(request.getTeam());

        UserEntity updatedUser = userRepository.save(user);

        log.info("Updated user id={}", updatedUser.getId());

        return userMapper.toResponse(updatedUser);
    }

    public void delete(Long id) {
        log.info("Deleting user id={}", id);
        userRepository.delete(findUser(id));
        log.info("Deleted user id={}", id);
    }

    private UserEntity findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
