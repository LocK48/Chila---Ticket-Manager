package com.chila.backend.sprint;

public class SprintNotFoundException extends RuntimeException {
    @Override public String getMessage() { return "Sprint was not found in this project."; }
}
