package com.chila.backend.workspace;

public class WorkspaceAccessDeniedException extends RuntimeException {
    public WorkspaceAccessDeniedException() {
        super("You do not have permission to perform this workspace action.");
    }
}
