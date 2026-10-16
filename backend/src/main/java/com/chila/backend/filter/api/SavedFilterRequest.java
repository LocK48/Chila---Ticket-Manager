package com.chila.backend.filter.api;

import java.time.LocalDate;
import java.util.UUID;
import com.chila.backend.filter.SavedFilterVisibility;
import com.chila.backend.issue.*;
import jakarta.validation.constraints.*;

public record SavedFilterRequest(@NotBlank @Size(max=100) String name, @NotNull SavedFilterVisibility visibility,
        IssueStatus status, IssuePriority priority, IssueType type, UUID assigneeId, boolean unassigned,
        UUID sprintId, UUID epicId, LocalDate dueFrom, LocalDate dueTo, @Size(max=200) String search) {}
