package com.chila.backend.issue.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.chila.backend.issue.IssuePriority;
import com.chila.backend.issue.IssueStatus;
import com.chila.backend.issue.IssueType;

public record IssueResponse(
        UUID id,
        UUID projectId,
        String projectKey,
        String issueKey,
        long issueNumber,
        String title,
        String description,
        IssueType type,
        IssueStatus status,
        IssuePriority priority,
        UUID reporterId,
        String reporterName,
        UUID assigneeId,
        String assigneeName,
        LocalDate dueDate,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt) {
}
