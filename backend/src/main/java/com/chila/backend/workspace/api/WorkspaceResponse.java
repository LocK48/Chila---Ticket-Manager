package com.chila.backend.workspace.api;

import java.time.Instant;
import java.util.UUID;

import com.chila.backend.workspace.MembershipRole;

public record WorkspaceResponse(
        UUID id,
        String name,
        String description,
        MembershipRole role,
        Instant createdAt,
        Instant updatedAt) {
}
