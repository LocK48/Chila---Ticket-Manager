package com.chila.backend.report.api;

import java.util.List;
import java.util.Map;
import com.chila.backend.issue.IssueStatus;

public record ProjectDashboardResponse(long totalIssues, long overdueIssues, Map<IssueStatus, Long> issuesByStatus,
        long completedLast30Days, Double averageLeadTimeDays, Double averageCycleTimeDays,
        List<SprintVelocityPoint> sprintVelocity) { }
