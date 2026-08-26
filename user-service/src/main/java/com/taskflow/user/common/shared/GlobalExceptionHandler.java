package com.taskflow.user.common.shared;


import com.taskflow.user.domain.exception.InvalidCredentialsException;
import com.taskflow.user.domain.exception.UserAlreadyExistsException;
import com.taskflow.user.domain.exception.UserNotFoundException;

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

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(
            UserNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(404).body(
                ApiErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(404)
                        .code("USER_NOT_FOUND")
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

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUserAlreadyExists(
            UserAlreadyExistsException ex, HttpServletRequest req) {

        log.warn("User already exists processing {} {}: {}",
                req.getMethod(),
                req.getRequestURI(),
                ex.getMessage());

        return ResponseEntity.status(409).body(
                ApiErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(409)
                        .code("USER_ALREADY_EXISTS")
                        .message(ex.getMessage())
                        .path(req.getRequestURI())
                        .build());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest req) {

        log.warn("Failed login attempt on {} {}", req.getMethod(), req.getRequestURI());

        return ResponseEntity.status(401).body(
                ApiErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(401)
                        .code("INVALID_CREDENTIALS")
                        .message(ex.getMessage())
                        .path(req.getRequestURI())
                        .build());
    }
}
