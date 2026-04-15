package com.taskflow.task.application;

import com.taskflow.task.application.mapper.TaskMapper;
import com.taskflow.task.controller.dto.CreateTaskRequest;
import com.taskflow.task.controller.dto.TaskResponse;
import com.taskflow.task.controller.dto.UpdateTaskRequest;
import com.taskflow.task.domain.TaskPriority;
import com.taskflow.task.domain.exception.TaskNotFoundException;
import com.taskflow.task.infrastructure.entity.TaskEntity;
import com.taskflow.task.infrastructure.repository.TaskRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    public TaskResponse createTask(CreateTaskRequest request) {
        log.info("Creating task with title='{}' priority='{}'",
                request.getTitle(), request.getTitle());
        if(request.getStatus() == null) {
            request.setStatus("todo");
        }
        if (request.getPriority() == null) {
            request.setPriority(TaskPriority.LOW);
        }
        TaskEntity saved = taskRepository.save(taskMapper.toEntity(request));

        log.info("Created task with id= '{}' ", saved.getId());
        return taskMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(Long id) {
        return taskMapper.toResponse(taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id)));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAll() {
        return taskRepository.findAll().stream().map(taskMapper::toResponse).toList();
    }

    @Transactional
    public TaskResponse update(Long id, UpdateTaskRequest request) {
        log.info("Updating task with id= '{}' ", id);
        TaskEntity entity = taskRepository.findById(id)
                .orElseThrow(() -> {
                        log.warn("Task not found for deletion id= '{}'", id);
                        return new TaskNotFoundException(id);
                        });

        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setStatus(request.getStatus());
        entity.setPriority(request.getPriority());
        entity.setAssigneeId(request.getAssigneeId());
        entity.setDueDate(request.getDueDate());

        TaskEntity updated = taskRepository.save(entity);
        log.info("Task created successfully id={}", updated.getId());
        return taskMapper.toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting task id={}", id);
        TaskEntity task = taskRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Task not found for deletion id={}", id);
                    return new TaskNotFoundException(id);
                });
        taskRepository.delete(task);
        log.info("Task deleted id={}", id);
    }
}
