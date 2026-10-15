package com.chila.backend.issue.api;

import java.util.List;

public record IssuePageResponse(
        List<IssueResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages) {
}
