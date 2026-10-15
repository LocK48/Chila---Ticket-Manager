package com.chila.backend.issue;

public class ArchivedProjectException extends RuntimeException {
    public ArchivedProjectException() {
        super("Restore the project before changing its issues.");
    }
}
