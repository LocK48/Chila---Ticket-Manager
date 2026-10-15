package com.chila.backend.issue;

public class IssueNotFoundException extends RuntimeException {
    public IssueNotFoundException() {
        super("Issue was not found.");
    }
}
