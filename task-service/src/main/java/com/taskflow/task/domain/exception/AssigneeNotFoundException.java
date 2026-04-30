package com.taskflow.task.domain.exception;

public class AssigneeNotFoundException extends RuntimeException {

    public AssigneeNotFoundException(Long id) {
        super("Assignee not found with id: " + id);
    }
}
