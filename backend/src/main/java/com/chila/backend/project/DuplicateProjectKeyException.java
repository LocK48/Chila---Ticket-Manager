package com.chila.backend.project;

public class DuplicateProjectKeyException extends RuntimeException {
    public DuplicateProjectKeyException() {
        super("A project with this key already exists in the workspace.");
    }
}
