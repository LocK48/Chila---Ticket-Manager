package com.chila.backend.comment.api;

import java.util.List;

public record IssueCommentPageResponse(List<IssueCommentResponse> items, int page, int size,
        long totalItems, int totalPages) { }
