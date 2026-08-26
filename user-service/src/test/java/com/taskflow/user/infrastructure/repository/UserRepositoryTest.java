package com.taskflow.user.infrastructure.repository;

import com.taskflow.user.infrastructure.entity.UserEntity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class UserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired UserRepository userRepository;

    @Test
    void save_persistsEntityWithAuditTimestamps() {
        UserEntity saved = userRepository.save(buildUser("Alice", "alice@example.com", "Backend"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getTeam()).isEqualTo("Backend");
    }

    @Test
    void existsByEmail_existingEmail_returnsTrue() {
        userRepository.save(buildUser("Alice", "alice@example.com", null));

        assertThat(userRepository.existsByEmail("alice@example.com")).isTrue();
    }

    @Test
    void existsByEmail_unknownEmail_returnsFalse() {
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void existsByEmail_afterDelete_returnsFalse() {
        UserEntity user = userRepository.save(buildUser("Alice", "alice@example.com", null));
        userRepository.delete(user);

        assertThat(userRepository.existsByEmail("alice@example.com")).isFalse();
    }

    private UserEntity buildUser(String name, String email, String team) {
        return UserEntity.builder()
                .name(name)
                .email(email)
                .team(team)
                .password("hashed-password")
                .role("USER")
                .build();
    }
}
