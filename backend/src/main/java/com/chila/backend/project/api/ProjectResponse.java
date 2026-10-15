package com.chila.backend.project.api;

import java.time.Instant;
import java.util.UUID;

import com.chila.backend.project.ProjectStatus;

public record ProjectResponse(
        UUID id,
        UUID workspaceId,
        UUID createdBy,
        String name,
        String key,
        String description,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
