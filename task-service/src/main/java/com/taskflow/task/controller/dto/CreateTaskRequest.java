package com.taskflow.task.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taskflow.task.domain.TaskPriority;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateTaskRequest {
    @JsonProperty("title")       private String title;
    @JsonProperty("description") private String description;
    @JsonProperty("assigneeId")  private Long assigneeId;
    @JsonProperty("priority")    private TaskPriority priority;
    @JsonProperty("status")      private String status;
    @JsonProperty("dueDate")     private LocalDate dueDate;
}