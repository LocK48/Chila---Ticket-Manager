package com.chila.backend.issue.api;

import java.util.UUID;

import com.chila.backend.issue.IssueStatus;
import jakarta.validation.constraints.NotNull;

public record MoveIssueRequest(
        @NotNull IssueStatus status,
        UUID beforeIssueId) {
}
