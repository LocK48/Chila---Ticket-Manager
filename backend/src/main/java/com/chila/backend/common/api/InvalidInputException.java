package com.chila.backend.common.api;

public class InvalidInputException extends RuntimeException {

    private final String field;

    public InvalidInputException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
