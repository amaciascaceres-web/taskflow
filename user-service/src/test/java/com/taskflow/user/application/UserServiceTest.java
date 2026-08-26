package com.taskflow.user.application;

import com.taskflow.user.application.mapper.UserMapper;
import com.taskflow.user.controller.dto.UserRequest;
import com.taskflow.user.controller.dto.UserResponse;
import com.taskflow.user.domain.exception.UserNotFoundException;
import com.taskflow.user.infrastructure.entity.UserEntity;
import com.taskflow.user.infrastructure.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @InjectMocks UserService userService;

    private static final UserEntity ENTITY = UserEntity.builder()
            .id(1L).name("Alice").email("alice@example.com").team("Backend")
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build();

    private static final UserResponse RESPONSE = new UserResponse(
            1L, "Alice", "Backend", "alice@example.com",
            LocalDateTime.now(), LocalDateTime.now());

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    void getAll_returnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(ENTITY));
        when(userMapper.toResponse(ENTITY)).thenReturn(RESPONSE);

        List<UserResponse> result = userService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("alice@example.com");
    }

    @Test
    void getAll_noUsers_returnsEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        assertThat(userService.getAll()).isEmpty();
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_found_returnsResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(ENTITY));
        when(userMapper.toResponse(ENTITY)).thenReturn(RESPONSE);

        assertThat(userService.getById(1L).email()).isEqualTo("alice@example.com");
    }

    @Test
    void getById_notFound_throwsUserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_found_updatesAllFieldsAndReturnsResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(ENTITY));
        when(userRepository.save(ENTITY)).thenReturn(ENTITY);
        when(userMapper.toResponse(ENTITY)).thenReturn(RESPONSE);

        UserRequest request = new UserRequest();
        request.setName("Bob");
        request.setEmail("bob@example.com");
        request.setTeam("Frontend");

        userService.update(1L, request);

        assertThat(ENTITY.getName()).isEqualTo("Bob");
        assertThat(ENTITY.getEmail()).isEqualTo("bob@example.com");
        assertThat(ENTITY.getTeam()).isEqualTo("Frontend");
    }

    @Test
    void update_notFound_throwsUserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(99L, validRequest()))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_found_deletesUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(ENTITY));

        userService.delete(1L);

        verify(userRepository).delete(ENTITY);
    }

    @Test
    void delete_notFound_throwsUserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).delete(any());
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
