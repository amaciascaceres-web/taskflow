package com.taskflow.common.exception;

import com.taskflow.task.domain.exception.TaskNotFoundException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            TaskNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(404).body(
                ApiErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(404)
                        .code("TASK_NOT_FOUND")
                        .message(ex.getMessage())
                        .path(req.getRequestURI())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<FieldError> errors = ex.getBindingResult()
                .getFieldErrors().stream()
                .map(e -> new FieldError(
                        e.getField(), e.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest().body(
                ApiErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(400)
                        .code("VALIDATION_ERROR")
                        .message("Validation failed")
                        .path(req.getRequestURI())
                        .errors(errors)
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest req) {

        log.error("Unexpected error processing {} {}: {}",
                req.getMethod(),
                req.getRequestURI(),
                ex.getMessage(),
                ex);

        return ResponseEntity.internalServerError().body(
                ApiErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(500)
                        .code("INTERNAL_ERROR")
                        .message("An unexpected error occurred")
                        .path(req.getRequestURI())
                        .build());
    }
}
