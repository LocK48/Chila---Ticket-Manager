package com.chila.backend.activity.api;

import java.time.Instant;
import java.util.UUID;

public record IssueActivityResponse(UUID id, UUID issueId, UUID actorId, String actorName, String action,
        String fieldName, String oldValue, String newValue, Instant createdAt) { }
