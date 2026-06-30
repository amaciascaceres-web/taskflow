package com.taskflow.task.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taskflow.task.domain.TaskPriority;
import com.taskflow.task.domain.TaskStatus;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateTaskRequest {
    @JsonProperty("title")
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Size(max = 2000, message = "Description must be less than 2000 characters")
    @JsonProperty("description") private String description;

    @JsonProperty("assigneeId")  private Long assigneeId;

    @NotNull(message = "Priority is required")
    @JsonProperty("priority")    private TaskPriority priority;


    @JsonProperty("status")      private TaskStatus status;

    @FutureOrPresent(message = "Due date must be in the future")
    @JsonProperty("dueDate")     private LocalDate dueDate;
}