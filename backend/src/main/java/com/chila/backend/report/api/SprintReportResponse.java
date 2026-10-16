package com.chila.backend.report.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.chila.backend.sprint.SprintState;

public record SprintReportResponse(UUID sprintId, String sprintName, SprintState state, LocalDate startDate, LocalDate endDate,
        boolean historyAvailable, Long committedIssues, Long completedIssues, Long unfinishedIssues,
        Double completionPercentage, List<BurndownPoint> burndown, List<SprintCompletionIssue> completionIssues) { }
