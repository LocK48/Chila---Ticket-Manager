package com.chila.backend.automation.api;

import java.time.Instant;
import java.util.UUID;
import com.chila.backend.automation.AutomationAction;
import com.chila.backend.issue.IssueStatus;

public record AutomationRuleResponse(UUID id, UUID projectId, String name, boolean enabled,
        IssueStatus targetStatus, AutomationAction action, Instant createdAt, Instant updatedAt) { }
