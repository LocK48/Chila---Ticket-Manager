package com.chila.backend.project;

public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException() {
        super("Project was not found.");
    }
}
