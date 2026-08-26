package com.taskflow.user.application;

import com.taskflow.user.application.mapper.UserMapper;
import com.taskflow.user.controller.dto.LoginRequest;
import com.taskflow.user.controller.dto.RegisterRequest;
import com.taskflow.user.controller.dto.TokenResponse;
import com.taskflow.user.controller.dto.UserResponse;
import com.taskflow.user.domain.exception.InvalidCredentialsException;
import com.taskflow.user.domain.exception.UserAlreadyExistsException;
import com.taskflow.user.infrastructure.entity.UserEntity;
import com.taskflow.user.infrastructure.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    // Registration never accepts a client-supplied role (see RegisterRequest) —
    // anything above USER must be granted through a separate, authenticated path.
    // Stored without the "ROLE_" prefix: it's added once, downstream, by
    // HeaderAuthenticationFilter when building the SimpleGrantedAuthority —
    // prefixing it here too would double it up (ROLE_ROLE_USER) and silently
    // break every hasRole(...) check in task-service.
    private static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserResponse register(RegisterRequest request) {
        log.info("Registering user with email='{}'", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        UserEntity user = UserEntity.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .team(request.getTeam())
                .role(DEFAULT_ROLE)
                .build();

        UserEntity savedUser = userRepository.save(user);

        log.info("Registered user with id={}", savedUser.getId());

        return userMapper.toResponse(savedUser);
    }

    public TokenResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .filter(u -> passwordEncoder.matches(request.getPassword(), u.getPassword()))
                .orElseThrow(InvalidCredentialsException::new);

        return new TokenResponse(jwtService.generateToken(user.getEmail(), user.getRole()));
    }
}
