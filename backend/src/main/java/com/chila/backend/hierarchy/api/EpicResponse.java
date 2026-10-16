package com.chila.backend.hierarchy.api;
import java.time.Instant;
import java.util.UUID;
public record EpicResponse(UUID id, UUID projectId, String name, String summary, String color,
        long totalIssues, long completedIssues, Instant createdAt, Instant updatedAt) { }
