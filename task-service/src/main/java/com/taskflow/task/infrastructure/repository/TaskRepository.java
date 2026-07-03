package com.taskflow.task.infrastructure.repository;

import com.taskflow.task.domain.TaskStatus;
import com.taskflow.task.infrastructure.entity.TaskEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TaskRepository extends JpaRepository<TaskEntity, Long>,
                                        JpaSpecificationExecutor<TaskEntity> {
    List<TaskEntity> findByStatus(TaskStatus status);
}
