package com.chila.backend.filter.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import com.chila.backend.filter.SavedFilterVisibility;
import com.chila.backend.issue.*;

public record SavedFilterResponse(UUID id, UUID projectId, UUID creatorId, String name, SavedFilterVisibility visibility,
        IssueStatus status, IssuePriority priority, IssueType type, UUID assigneeId, boolean unassigned,
        UUID sprintId, UUID epicId, LocalDate dueFrom, LocalDate dueTo, String search, Instant createdAt, Instant updatedAt) {}
