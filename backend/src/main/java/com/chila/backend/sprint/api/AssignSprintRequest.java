package com.chila.backend.sprint.api;

import java.util.UUID;

public record AssignSprintRequest(UUID sprintId, UUID beforeIssueId) { }
