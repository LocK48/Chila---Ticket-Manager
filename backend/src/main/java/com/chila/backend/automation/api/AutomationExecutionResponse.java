package com.chila.backend.automation.api;

import java.time.Instant;
import java.util.UUID;
import com.chila.backend.automation.AutomationOutcome;

public record AutomationExecutionResponse(UUID id, UUID ruleId, String ruleName, String issueKey,
        AutomationOutcome outcome, String detail, Instant executedAt) { }
