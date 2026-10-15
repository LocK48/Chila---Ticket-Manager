package com.chila.backend.workspace;

public class WorkspaceNotFoundException extends RuntimeException {
    public WorkspaceNotFoundException() {
        super("Workspace was not found.");
    }
}
