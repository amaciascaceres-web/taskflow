package com.taskflow.task.infrastructure.repository;

import com.taskflow.task.domain.TaskPriority;
import com.taskflow.task.domain.TaskStatus;
import com.taskflow.task.infrastructure.entity.TaskEntity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskSpecificationTest {

    @Mock Root<TaskEntity> root;
    @Mock CriteriaQuery<?> query;
    @Mock CriteriaBuilder cb;
    @Mock Path<Object> path;
    @Mock Predicate predicate;

    // ── hasStatusIn ───────────────────────────────────────────────────────────

    @Test
    void hasStatusIn_nullInput_returnsNullPredicate() {
        assertThat(TaskSpecification.hasStatusIn(null).toPredicate(root, query, cb)).isNull();
        verifyNoInteractions(root, cb);
    }

    @Test
    void hasStatusIn_emptyList_returnsNullPredicate() {
        assertThat(TaskSpecification.hasStatusIn(List.of()).toPredicate(root, query, cb)).isNull();
        verifyNoInteractions(root, cb);
    }

    @Test
    void hasStatusIn_nonEmptyList_returnsInPredicate() {
        when(root.get("status")).thenReturn(path);
        when(path.in(List.of(TaskStatus.TODO))).thenReturn(predicate);

        Predicate result = TaskSpecification.hasStatusIn(List.of(TaskStatus.TODO)).toPredicate(root, query, cb);

        assertThat(result).isEqualTo(predicate);
    }

    @Test
    void hasStatusIn_multipleValues_returnsInPredicate() {
        when(root.get("status")).thenReturn(path);
        when(path.in(List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS))).thenReturn(predicate);

        Predicate result = TaskSpecification
                .hasStatusIn(List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS)).toPredicate(root, query, cb);

        assertThat(result).isEqualTo(predicate);
    }

    // ── hasAssigneeIdIn ───────────────────────────────────────────────────────

    @Test
    void hasAssigneeIdIn_nullInput_returnsNullPredicate() {
        assertThat(TaskSpecification.hasAssigneeIdIn(null).toPredicate(root, query, cb)).isNull();
        verifyNoInteractions(root, cb);
    }

    @Test
    void hasAssigneeIdIn_emptyList_returnsNullPredicate() {
        assertThat(TaskSpecification.hasAssigneeIdIn(List.of()).toPredicate(root, query, cb)).isNull();
        verifyNoInteractions(root, cb);
    }

    @Test
    void hasAssigneeIdIn_nonEmptyList_returnsInPredicate() {
        when(root.get("assigneeId")).thenReturn(path);
        when(path.in(List.of(1L, 2L))).thenReturn(predicate);

        Predicate result = TaskSpecification.hasAssigneeIdIn(List.of(1L, 2L)).toPredicate(root, query, cb);

        assertThat(result).isEqualTo(predicate);
    }

    // ── hasPriorityIn ─────────────────────────────────────────────────────────

    @Test
    void hasPriorityIn_nullInput_returnsNullPredicate() {
        assertThat(TaskSpecification.hasPriorityIn(null).toPredicate(root, query, cb)).isNull();
        verifyNoInteractions(root, cb);
    }

    @Test
    void hasPriorityIn_emptyList_returnsNullPredicate() {
        assertThat(TaskSpecification.hasPriorityIn(List.of()).toPredicate(root, query, cb)).isNull();
        verifyNoInteractions(root, cb);
    }

    @Test
    void hasPriorityIn_nonEmptyList_returnsInPredicate() {
        when(root.get("priority")).thenReturn(path);
        when(path.in(List.of(TaskPriority.HIGH, TaskPriority.MEDIUM))).thenReturn(predicate);

        Predicate result = TaskSpecification
                .hasPriorityIn(List.of(TaskPriority.HIGH, TaskPriority.MEDIUM)).toPredicate(root, query, cb);

        assertThat(result).isEqualTo(predicate);
    }

    // ── null-safety contract ──────────────────────────────────────────────────

    @Test
    void allSpecifications_returnNonNullSpecificationObject() {
        assertThat(TaskSpecification.hasStatusIn(null)).isNotNull();
        assertThat(TaskSpecification.hasAssigneeIdIn(null)).isNotNull();
        assertThat(TaskSpecification.hasPriorityIn(null)).isNotNull();
    }
}
