package com.chila.backend.workspace.api;

import java.time.Instant;
import java.util.UUID;

import com.chila.backend.workspace.MembershipRole;

public record MemberResponse(
        UUID userId,
        String email,
        String displayName,
        MembershipRole role,
        Instant joinedAt) {
}
