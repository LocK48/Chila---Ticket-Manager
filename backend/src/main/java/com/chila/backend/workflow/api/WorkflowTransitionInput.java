package com.chila.backend.workflow.api;

import com.chila.backend.issue.IssueStatus;
import jakarta.validation.constraints.NotNull;

public record WorkflowTransitionInput(@NotNull IssueStatus from, @NotNull IssueStatus to) {}
