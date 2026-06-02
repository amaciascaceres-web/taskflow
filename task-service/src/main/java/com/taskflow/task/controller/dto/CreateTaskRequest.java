package com.taskflow.task.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taskflow.task.domain.TaskPriority;

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
public class CreateTaskRequest {
    @JsonProperty("title")
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be between 3 and 255 characters")
    private String title;

    @Size(max = 2000, message = "Description must be less than 20000 characters")
    @JsonProperty("description") private String description;

    @JsonProperty("assigneeId")  private Long assigneeId;

    @NotNull(message = "Priority is required")
    @JsonProperty("priority")    private TaskPriority priority;


    @JsonProperty("status")      private String status;

    @FutureOrPresent(message = "Due date must be in the future")
    @JsonProperty("dueDate")     private LocalDate dueDate;
}