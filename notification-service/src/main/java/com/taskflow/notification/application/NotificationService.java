package com.taskflow.notification.application;

import com.taskflow.notification.infrastructure.TaskCreatedEvent;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotificationService {

    public void handleTaskCreated(TaskCreatedEvent event) {
        //We just register the log by now. Later we will send the notification.
        log.info("Notification sent: task created taskId={} assigneeId={} title='{}'", event.taskId(), event.assigneeId(), event.title());
    }
}
