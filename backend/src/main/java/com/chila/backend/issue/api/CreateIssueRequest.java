package com.chila.backend.issue.api;

import java.time.LocalDate;
import java.util.UUID;

import com.chila.backend.issue.IssuePriority;
import com.chila.backend.issue.IssueStatus;
import com.chila.backend.issue.IssueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIssueRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 10000) String description,
        @NotNull IssueType type,
        IssueStatus status,
        IssuePriority priority,
        UUID assigneeId,
        LocalDate dueDate) {
}
