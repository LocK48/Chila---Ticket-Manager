package com.chila.backend.report;

import java.time.Instant;
import java.util.UUID;
import com.chila.backend.issue.IssueEntity;
import com.chila.backend.issue.IssueStatus;
import com.chila.backend.sprint.SprintEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "sprint_report_events", indexes = {
        @Index(name = "ix_sprint_report_events_sprint_time", columnList = "sprint_id, occurred_at, event_id"),
        @Index(name = "ix_sprint_report_events_issue", columnList = "sprint_id, issue_id, occurred_at, event_id")
})
public class SprintReportEventEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "event_id") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sprint_id", nullable = false) private SprintEntity sprint;
    @Column(name = "issue_id", nullable = false) private UUID issueId;
    @Column(name = "issue_key", nullable = false, length = 32) private String issueKey;
    @Column(name = "issue_title", nullable = false, length = 200) private String issueTitle;
    @Enumerated(EnumType.STRING) @Column(name = "issue_status", nullable = false, length = 24) private IssueStatus issueStatus;
    @Enumerated(EnumType.STRING) @Column(name = "event_kind", nullable = false, length = 24) private SprintReportEventKind eventKind;
    @Column(name = "in_scope", nullable = false) private boolean inScope;
    @CreationTimestamp @Column(name = "occurred_at", nullable = false, updatable = false) private Instant occurredAt;

    protected SprintReportEventEntity() { }
    public SprintReportEventEntity(SprintEntity sprint, IssueEntity issue, SprintReportEventKind eventKind, boolean inScope) {
        this.sprint = sprint; this.issueId = issue.getId(); this.issueKey = issue.getProject().getKey() + "-" + issue.getIssueNumber();
        this.issueTitle = issue.getTitle(); this.issueStatus = issue.getStatus(); this.eventKind = eventKind; this.inScope = inScope;
    }
    public Long getId() { return id; }
    public UUID getIssueId() { return issueId; }
    public String getIssueKey() { return issueKey; }
    public String getIssueTitle() { return issueTitle; }
    public IssueStatus getIssueStatus() { return issueStatus; }
    public SprintReportEventKind getEventKind() { return eventKind; }
    public boolean isInScope() { return inScope; }
    public Instant getOccurredAt() { return occurredAt; }
}
