package com.chila.backend.comment.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateIssueCommentRequest(@NotBlank @Size(max = 5000) String body) { }
