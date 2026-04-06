package com.taskflow.task.application;

import com.taskflow.task.application.mapper.TaskMapper;
import com.taskflow.task.controller.dto.CreateTaskRequest;
import com.taskflow.task.controller.dto.TaskResponse;
import com.taskflow.task.infrastructure.repository.TaskRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    @Autowired
    private final TaskRepository taskRepository;

    private final TaskMapper taskMapper = new TaskMapper();

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public TaskResponse createTask(CreateTaskRequest request) {
            return taskMapper.toResponse(taskRepository.save(taskMapper.toEntity(request)));
    }
}
