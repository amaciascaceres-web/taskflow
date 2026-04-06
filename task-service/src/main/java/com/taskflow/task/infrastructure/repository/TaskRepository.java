package com.taskflow.task.infrastructure.repository;

import com.taskflow.task.infrastructure.entity.TaskEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

}
