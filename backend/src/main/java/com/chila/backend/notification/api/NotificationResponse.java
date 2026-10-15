package com.chila.backend.notification.api;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, UUID issueId, String issueTitle, String eventType,
        String message, Instant createdAt, Instant readAt) { }
