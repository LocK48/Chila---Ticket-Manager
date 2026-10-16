package com.chila.backend.report.api;

public interface IssueFlowMetrics {
    Double getAverageLeadTimeDays();
    Double getAverageCycleTimeDays();
    Long getCompletedLast30Days();
}
