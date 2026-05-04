package com.taskflow.task.domain.exception;

public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String serviceName) {
        super("Service unavailable: " + serviceName);
    }
}
