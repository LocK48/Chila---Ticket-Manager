package com.chila.backend.hierarchy.api;
import java.time.Instant;
import java.util.UUID;
import com.chila.backend.hierarchy.IssueLinkType;
public record IssueLinkResponse(UUID id, UUID sourceIssueId, String sourceIssueKey, String sourceTitle,
        UUID targetIssueId, String targetIssueKey, String targetTitle, IssueLinkType type, Instant createdAt) { }
