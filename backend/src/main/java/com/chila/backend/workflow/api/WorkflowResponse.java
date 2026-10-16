package com.chila.backend.workflow.api;

import java.util.List;
import com.chila.backend.issue.IssueStatus;

public record WorkflowResponse(List<IssueStatus> statuses, List<WorkflowTransitionInput> transitions) {}
