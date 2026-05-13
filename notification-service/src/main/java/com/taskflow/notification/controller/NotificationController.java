package com.taskflow.notification.controller;

import com.taskflow.notification.application.NotificationService;
import com.taskflow.notification.infrastructure.TaskCreatedEvent;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/task-created")
    public ResponseEntity<Void> taskCreated(
            @RequestBody TaskCreatedEvent event) {
        notificationService.handleTaskCreated(event);
        return ResponseEntity.ok().build();
    }
}

