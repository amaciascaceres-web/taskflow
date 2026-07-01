package com.taskflow.task.infrastructure.repository;

import com.taskflow.task.domain.TaskPriority;
import com.taskflow.task.domain.TaskStatus;
import com.taskflow.task.infrastructure.entity.TaskEntity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class TaskRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired TaskRepository taskRepository;

    @Test
    void save_persistsEntityWithAuditTimestamps() {
        TaskEntity task = buildTask("My task", TaskStatus.TODO, TaskPriority.HIGH);

        TaskEntity saved = taskRepository.save(task);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByStatus_returnsOnlyMatchingTasks() {
        taskRepository.save(buildTask("Todo task 1", TaskStatus.TODO, TaskPriority.HIGH));
        taskRepository.save(buildTask("Todo task 2", TaskStatus.TODO, TaskPriority.LOW));
        taskRepository.save(buildTask("In progress", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM));
        taskRepository.save(buildTask("Done task",   TaskStatus.DONE, TaskPriority.LOW));

        List<TaskEntity> todos = taskRepository.findByStatus(TaskStatus.TODO);

        assertThat(todos).hasSize(2)
                .allMatch(t -> t.getStatus() == TaskStatus.TODO);
    }

    @Test
    void findByStatus_noMatchingTasks_returnsEmptyList() {
        taskRepository.save(buildTask("In progress", TaskStatus.IN_PROGRESS, TaskPriority.HIGH));

        List<TaskEntity> result = taskRepository.findByStatus(TaskStatus.DONE);

        assertThat(result).isEmpty();
    }

    @Test
    void findByStatus_afterStatusChange_reflectsUpdate() {
        TaskEntity task = taskRepository.save(buildTask("Task", TaskStatus.TODO, TaskPriority.HIGH));

        task.setStatus(TaskStatus.IN_PROGRESS);
        taskRepository.save(task);

        assertThat(taskRepository.findByStatus(TaskStatus.TODO)).isEmpty();
        assertThat(taskRepository.findByStatus(TaskStatus.IN_PROGRESS)).hasSize(1);
    }

    private TaskEntity buildTask(String title, TaskStatus status, TaskPriority priority) {
        return TaskEntity.builder()
                .title(title)
                .status(status)
                .priority(priority)
                .build();
    }
}