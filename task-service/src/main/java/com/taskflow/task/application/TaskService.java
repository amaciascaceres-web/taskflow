package com.taskflow.task.application;

import com.taskflow.task.application.mapper.TaskMapper;
import com.taskflow.task.controller.dto.CreateTaskRequest;
import com.taskflow.task.controller.dto.TaskResponse;
import com.taskflow.task.controller.dto.UpdateTaskRequest;
import com.taskflow.task.domain.exception.TaskNotFoundException;
import com.taskflow.task.infrastructure.entity.TaskEntity;
import com.taskflow.task.infrastructure.repository.TaskRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    public TaskResponse createTask(CreateTaskRequest request) {
        return taskMapper.toResponse(taskRepository.save(taskMapper.toEntity(request)));
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
        TaskEntity entity = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setStatus(request.getStatus());
        entity.setPriority(request.getPriority());
        entity.setAssigneeId(request.getAssigneeId());
        entity.setDueDate(request.getDueDate());

        return taskMapper.toResponse(taskRepository.save(entity));
    }

    public void delete(Long id) {
        TaskEntity entity = taskRepository.findById(id)
                .orElseThrow( (() -> new TaskNotFoundException(id)));
        taskRepository.delete(entity);
    }
}
