package com.chila.backend.comment.api;

import java.time.Instant;
import java.util.UUID;

public record IssueCommentResponse(UUID id, UUID issueId, UUID authorId, String authorName, String body, Instant createdAt) { }
