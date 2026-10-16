package com.chila.backend.report;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.chila.backend.issue.*;
import com.chila.backend.project.*;
import com.chila.backend.report.api.*;
import com.chila.backend.sprint.*;
import com.chila.backend.workspace.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Bangkok");
    private static final int MAX_CHART_DAYS = 90;
    private final ProjectRepository projects;
    private final MembershipRepository memberships;
    private final IssueRepository issues;
    private final SprintRepository sprints;
    private final SprintReportEventRepository events;

    public ReportService(ProjectRepository projects, MembershipRepository memberships, IssueRepository issues,
            SprintRepository sprints, SprintReportEventRepository events) {
        this.projects = projects; this.memberships = memberships; this.issues = issues;
        this.sprints = sprints; this.events = events;
    }

    @Transactional(readOnly = true)
    public ProjectDashboardResponse dashboard(UUID actor, UUID projectId) {
        memberProject(actor, projectId);
        EnumMap<IssueStatus, Long> byStatus = new EnumMap<>(IssueStatus.class);
        long total = 0;
        for (IssueStatus status : IssueStatus.values()) {
            long count = issues.countByProject_IdAndStatusAndArchivedAtIsNull(projectId, status);
            byStatus.put(status, count); total += count;
        }
        long overdue = issues.countByProject_IdAndArchivedAtIsNullAndDueDateBefore(projectId, LocalDate.now(REPORT_ZONE));
        IssueFlowMetrics metrics = events.issueFlowMetrics(projectId);
        List<SprintVelocityPoint> velocity = events.velocityForProject(projectId).stream().map(row ->
                new SprintVelocityPoint(row.getSprintId(), row.getSprintName(), row.getStartDate(),
                        row.getHistoryAvailable() ? row.getCommittedIssues() : null,
                        row.getHistoryAvailable() ? row.getCompletedIssues() : null,
                        row.getHistoryAvailable() ? row.getUnfinishedIssues() : null,
                        row.getHistoryAvailable() ? row.getCompletionPercentage() : null,
                        Boolean.TRUE.equals(row.getHistoryAvailable()))).toList();
        return new ProjectDashboardResponse(total, overdue, Map.copyOf(byStatus),
                metrics.getCompletedLast30Days() == null ? 0 : metrics.getCompletedLast30Days(),
                metrics.getAverageLeadTimeDays(), metrics.getAverageCycleTimeDays(), velocity);
    }

    @Transactional(readOnly = true)
    public SprintReportResponse sprintReport(UUID actor, UUID projectId, UUID sprintId) {
        memberProject(actor, projectId);
        SprintEntity sprint = sprints.findByIdAndProject_Id(sprintId, projectId).orElseThrow(SprintNotFoundException::new);
        List<SprintReportEventEntity> history = events.findBySprint_IdOrderByOccurredAtAscIdAsc(sprintId);
        List<SprintReportEventEntity> completion = history.stream()
                .filter(e -> e.getEventKind() == SprintReportEventKind.SPRINT_COMPLETED).toList();
        boolean hasStart = history.stream().anyMatch(e -> e.getEventKind() == SprintReportEventKind.SPRINT_START);
        Map<UUID, SprintReportEventEntity> finalState = latestState(history);
        long scope = completion.isEmpty() ? finalState.values().stream().filter(SprintReportEventEntity::isInScope).count() : completion.size();
        long completed = completion.isEmpty()
                ? finalState.values().stream().filter(e -> e.isInScope() && e.getIssueStatus() == IssueStatus.DONE).count()
                : completion.stream().filter(e -> e.getIssueStatus() == IssueStatus.DONE).count();
        long unfinished = completion.isEmpty() ? Math.max(0, scope - completed) : completion.size() - completed;
        boolean completionAvailable = !completion.isEmpty();
        List<SprintCompletionIssue> completionIssues = completion.stream().map(e ->
                new SprintCompletionIssue(e.getIssueId(), e.getIssueKey(), e.getIssueTitle(), e.getIssueStatus())).toList();
        Double percent = scope == 0 ? null : 100.0 * completed / scope;
        return new SprintReportResponse(sprint.getId(), sprint.getName(), sprint.getState(), sprint.getStartDate(),
                sprint.getEndDate(), hasStart, hasStart ? scope : null, completionAvailable ? completed : null,
                completionAvailable ? unfinished : null, completionAvailable ? percent : null,
                hasStart ? burndown(history, sprint.getState()) : List.of(), completionIssues);
    }

    private List<BurndownPoint> burndown(List<SprintReportEventEntity> events, SprintState state) {
        List<SprintReportEventEntity> starts = events.stream()
                .filter(e -> e.getEventKind() == SprintReportEventKind.SPRINT_START).toList();
        if (starts.isEmpty()) return List.of();
        LocalDate firstDay = starts.get(0).getOccurredAt().atZone(REPORT_ZONE).toLocalDate();
        List<SprintReportEventEntity> completions = events.stream()
                .filter(e -> e.getEventKind() == SprintReportEventKind.SPRINT_COMPLETED).toList();
        LocalDate lastDay = state == SprintState.CLOSED && !completions.isEmpty()
                ? completions.get(completions.size() - 1).getOccurredAt().atZone(REPORT_ZONE).toLocalDate()
                : LocalDate.now(REPORT_ZONE);
        if (lastDay.isBefore(firstDay)) lastDay = firstDay;
        LocalDate chartStart = lastDay.isAfter(firstDay.plusDays(MAX_CHART_DAYS - 1L))
                ? lastDay.minusDays(MAX_CHART_DAYS - 1L) : firstDay;
        Map<UUID, SprintReportEventEntity> stateByIssue = new HashMap<>();
        int eventIndex = 0;
        Instant chartStartInstant = chartStart.atStartOfDay(REPORT_ZONE).toInstant();
        while (eventIndex < events.size() && events.get(eventIndex).getOccurredAt().isBefore(chartStartInstant)) {
            stateByIssue.put(events.get(eventIndex).getIssueId(), events.get(eventIndex++));
        }
        List<BurndownPoint> points = new ArrayList<>();
        for (LocalDate date = chartStart; !date.isAfter(lastDay); date = date.plusDays(1)) {
            Instant nextDay = date.plusDays(1).atStartOfDay(REPORT_ZONE).toInstant();
            while (eventIndex < events.size() && events.get(eventIndex).getOccurredAt().isBefore(nextDay)) {
                SprintReportEventEntity event = events.get(eventIndex++);
                stateByIssue.put(event.getIssueId(), event);
            }
            long currentScope = stateByIssue.values().stream().filter(SprintReportEventEntity::isInScope).count();
            long remaining = stateByIssue.values().stream()
                    .filter(SprintReportEventEntity::isInScope)
                    .filter(e -> e.getIssueStatus() != IssueStatus.DONE).count();
            points.add(new BurndownPoint(date, currentScope, remaining));
        }
        return points;
    }

    private static Map<UUID, SprintReportEventEntity> latestState(List<SprintReportEventEntity> history) {
        return history.stream().collect(Collectors.toMap(SprintReportEventEntity::getIssueId, Function.identity(),
                (earlier, later) -> later, HashMap::new));
    }

    private ProjectEntity memberProject(UUID actor, UUID id) {
        ProjectEntity project = projects.findById(id).orElseThrow(ProjectNotFoundException::new);
        if (!memberships.existsByWorkspaceIdAndUserId(project.getWorkspace().getId(), actor))
            throw new ProjectNotFoundException();
        return project;
    }
}
