package com.chila.backend.issue;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException() {
        super("The requested status change is not allowed from the current status.");
    }
}
