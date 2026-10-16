package com.chila.backend.report;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.chila.backend.report.api.IssueFlowMetrics;
import com.chila.backend.report.api.SprintVelocityProjection;

public interface SprintReportEventRepository extends JpaRepository<SprintReportEventEntity, Long> {
    List<SprintReportEventEntity> findBySprint_IdOrderByOccurredAtAscIdAsc(UUID sprintId);

    @Query(value = "select s.id as \"sprintId\", s.name as \"sprintName\", s.start_date as \"startDate\", "
            + "count(e.event_id) as \"committedIssues\", "
            + "count(e.event_id) filter (where e.issue_status = 'DONE') as \"completedIssues\", "
            + "count(e.event_id) filter (where e.issue_status <> 'DONE') as \"unfinishedIssues\", "
            + "case when count(e.event_id) = 0 then null else "
            + "100.0 * count(e.event_id) filter (where e.issue_status = 'DONE') / count(e.event_id) end as \"completionPercentage\", "
            + "(count(e.event_id) > 0) as \"historyAvailable\" "
            + "from sprints s left join sprint_report_events e on e.sprint_id = s.id and e.event_kind = 'SPRINT_COMPLETED' "
            + "where s.project_id = :projectId and s.state = 'CLOSED' "
            + "group by s.id order by s.start_date desc nulls last, s.created_at desc limit 20", nativeQuery = true)
    List<SprintVelocityProjection> velocityForProject(@Param("projectId") UUID projectId);

    @Query(value = "with completion as (select i.id issue_id, i.created_at, "
            + "min(a.created_at) filter (where a.new_value = 'IN_PROGRESS') first_started_at, "
            + "min(a.created_at) filter (where a.new_value = 'DONE') first_done_at "
            + "from issues i left join issue_activities a on a.issue_id = i.id and a.field_name = 'status' "
            + "and a.new_value in ('IN_PROGRESS','DONE') where i.project_id = :projectId and i.archived_at is null "
            + "group by i.id, i.created_at), latest as (select * from completion where first_done_at is not null "
            + "order by first_done_at desc limit 100) "
            + "select avg(extract(epoch from (first_done_at - created_at)) / 86400.0) as \"averageLeadTimeDays\", "
            + "avg(extract(epoch from (first_done_at - first_started_at)) / 86400.0) "
            + "filter (where first_started_at is not null and first_started_at <= first_done_at) as \"averageCycleTimeDays\", "
            + "(select count(distinct issue_id) from issue_activities a join issues i on i.id = a.issue_id "
            + "where i.project_id = :projectId and i.archived_at is null and a.field_name = 'status' "
            + "and a.new_value = 'DONE' and a.created_at >= now() - interval '30 days') as \"completedLast30Days\" "
            + "from latest", nativeQuery = true)
    IssueFlowMetrics issueFlowMetrics(@Param("projectId") UUID projectId);
}
