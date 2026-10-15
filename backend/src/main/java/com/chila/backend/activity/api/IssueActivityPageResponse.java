package com.chila.backend.activity.api;

import java.util.List;

public record IssueActivityPageResponse(List<IssueActivityResponse> items, int page, int size,
        long totalItems, int totalPages) { }
