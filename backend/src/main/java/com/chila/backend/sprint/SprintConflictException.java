package com.chila.backend.sprint;

public class SprintConflictException extends RuntimeException {
    private final String message;
    public SprintConflictException(String message) { this.message = message; }
    @Override public String getMessage() { return message; }
}
