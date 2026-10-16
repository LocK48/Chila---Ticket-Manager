package com.chila.backend.automation.api;

import com.chila.backend.automation.AutomationAction;
import com.chila.backend.issue.IssueStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AutomationRuleRequest(@NotBlank @Size(max = 100) String name, boolean enabled,
        @NotNull IssueStatus targetStatus, @NotNull AutomationAction action) { }
