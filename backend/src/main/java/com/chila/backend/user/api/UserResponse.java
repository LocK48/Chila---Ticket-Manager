package com.chila.backend.user.api;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String email, String displayName, Instant createdAt) {
}
