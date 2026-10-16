package com.chila.backend.report.api;

import java.time.LocalDate;
import java.util.UUID;

public record SprintVelocityPoint(UUID sprintId, String sprintName, LocalDate startDate,
        Long committedIssues, Long completedIssues, Long unfinishedIssues, Double completionPercentage,
        boolean historyAvailable) { }
