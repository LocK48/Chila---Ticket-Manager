package com.chila.backend.issue;

public class IssueArchivedException extends RuntimeException {
    public IssueArchivedException() {
        super("Restore this issue before editing it.");
    }
}
