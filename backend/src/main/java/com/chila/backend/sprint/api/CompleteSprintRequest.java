package com.chila.backend.sprint.api;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CompleteSprintRequest(@NotNull UnfinishedIssueAction unfinishedIssueAction, UUID targetSprintId) {
    public enum UnfinishedIssueAction { BACKLOG, FUTURE_SPRINT }
}
