package com.taskflow.user.domain.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        // Deliberately generic — doesn't reveal whether the email exists
        super("Invalid email or password");
    }
}
