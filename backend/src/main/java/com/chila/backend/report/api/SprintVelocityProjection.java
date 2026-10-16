package com.chila.backend.report.api;

import java.time.LocalDate;
import java.util.UUID;

public interface SprintVelocityProjection {
    UUID getSprintId();
    String getSprintName();
    LocalDate getStartDate();
    Long getCommittedIssues();
    Long getCompletedIssues();
    Long getUnfinishedIssues();
    Double getCompletionPercentage();
    Boolean getHistoryAvailable();
}
