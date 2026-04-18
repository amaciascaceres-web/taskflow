package com.taskflow.task.shared;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;

@Builder
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldError> errors
){}
