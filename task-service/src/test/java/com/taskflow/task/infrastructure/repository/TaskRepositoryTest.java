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

import org.springframework.data.jpa.domain.Specification;

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

    // ── Specification-based filtering ─────────────────────────────────────────

    @Test
    void findAll_withAssigneeIdSpec_returnsOnlyTasksForThatAssignee() {
        taskRepository.save(buildTaskForAssignee("Task A", TaskStatus.TODO, TaskPriority.HIGH, 1L));
        taskRepository.save(buildTaskForAssignee("Task B", TaskStatus.TODO, TaskPriority.LOW, 2L));

        List<TaskEntity> result = taskRepository.findAll(TaskSpecification.hasAssigneeIdIn(List.of(1L)));

        assertThat(result).hasSize(1)
                .allMatch(t -> Long.valueOf(1L).equals(t.getAssigneeId()));
    }

    @Test
    void findAll_withMultipleAssigneeIds_returnsTasksForAllAssignees() {
        taskRepository.save(buildTaskForAssignee("Task A", TaskStatus.TODO, TaskPriority.HIGH, 1L));
        taskRepository.save(buildTaskForAssignee("Task B", TaskStatus.TODO, TaskPriority.LOW,  2L));
        taskRepository.save(buildTaskForAssignee("Task C", TaskStatus.TODO, TaskPriority.HIGH, 3L));

        List<TaskEntity> result = taskRepository.findAll(TaskSpecification.hasAssigneeIdIn(List.of(1L, 2L)));

        assertThat(result).hasSize(2)
                .allMatch(t -> List.of(1L, 2L).contains(t.getAssigneeId()));
    }

    @Test
    void findAll_withStatusAndAssigneeIdSpec_returnsCombinedFilter() {
        taskRepository.save(buildTaskForAssignee("Task A", TaskStatus.TODO,        TaskPriority.HIGH, 1L));
        taskRepository.save(buildTaskForAssignee("Task B", TaskStatus.IN_PROGRESS, TaskPriority.LOW,  1L));
        taskRepository.save(buildTaskForAssignee("Task C", TaskStatus.TODO,        TaskPriority.HIGH, 2L));

        Specification<TaskEntity> spec = Specification
                .where(TaskSpecification.hasStatusIn(List.of(TaskStatus.TODO)))
                .and(TaskSpecification.hasAssigneeIdIn(List.of(1L)));

        List<TaskEntity> result = taskRepository.findAll(spec);

        assertThat(result).hasSize(1)
                .allMatch(t -> t.getStatus() == TaskStatus.TODO && Long.valueOf(1L).equals(t.getAssigneeId()));
    }

    @Test
    void findAll_withMultipleStatuses_returnsTasksMatchingAnyStatus() {
        taskRepository.save(buildTask("Todo task",        TaskStatus.TODO,        TaskPriority.HIGH));
        taskRepository.save(buildTask("In-progress task", TaskStatus.IN_PROGRESS, TaskPriority.LOW));
        taskRepository.save(buildTask("Done task",        TaskStatus.DONE,        TaskPriority.LOW));

        List<TaskEntity> result = taskRepository.findAll(
                TaskSpecification.hasStatusIn(List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS)));

        assertThat(result).hasSize(2)
                .allMatch(t -> t.getStatus() == TaskStatus.TODO || t.getStatus() == TaskStatus.IN_PROGRESS);
    }

    @Test
    void findAll_withPrioritySpec_returnsOnlyMatchingPriority() {
        taskRepository.save(buildTask("High task",   TaskStatus.TODO, TaskPriority.HIGH));
        taskRepository.save(buildTask("Low task",    TaskStatus.TODO, TaskPriority.LOW));
        taskRepository.save(buildTask("Medium task", TaskStatus.TODO, TaskPriority.MEDIUM));

        List<TaskEntity> result = taskRepository.findAll(
                TaskSpecification.hasPriorityIn(List.of(TaskPriority.HIGH)));

        assertThat(result).hasSize(1)
                .allMatch(t -> t.getPriority() == TaskPriority.HIGH);
    }

    @Test
    void findAll_withAllNullFilters_returnsAllTasks() {
        taskRepository.save(buildTask("Task A", TaskStatus.TODO,        TaskPriority.HIGH));
        taskRepository.save(buildTask("Task B", TaskStatus.IN_PROGRESS, TaskPriority.LOW));

        Specification<TaskEntity> spec = Specification
                .where(TaskSpecification.hasStatusIn(null))
                .and(TaskSpecification.hasAssigneeIdIn(null))
                .and(TaskSpecification.hasPriorityIn(null));

        List<TaskEntity> result = taskRepository.findAll(spec);

        assertThat(result).hasSize(2);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private TaskEntity buildTask(String title, TaskStatus status, TaskPriority priority) {
        return TaskEntity.builder()
                .title(title)
                .status(status)
                .priority(priority)
                .build();
    }

    private TaskEntity buildTaskForAssignee(String title, TaskStatus status, TaskPriority priority, Long assigneeId) {
        return TaskEntity.builder()
                .title(title)
                .status(status)
                .priority(priority)
                .assigneeId(assigneeId)
                .build();
    }
}