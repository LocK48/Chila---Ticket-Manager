package com.chila.backend.report.api;

import java.util.UUID;
import com.chila.backend.issue.IssueStatus;

public record SprintCompletionIssue(UUID issueId, String issueKey, String title, IssueStatus status) { }
