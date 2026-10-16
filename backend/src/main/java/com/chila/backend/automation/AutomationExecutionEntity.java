package com.chila.backend.automation;

import java.time.Instant;
import java.util.UUID;
import com.chila.backend.issue.IssueEntity;
import com.chila.backend.project.ProjectEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "automation_executions", indexes = @Index(name = "ix_automation_executions_project_time", columnList = "project_id, executed_at"))
public class AutomationExecutionEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false) private ProjectEntity project;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "rule_id") private AutomationRuleEntity rule;
    @Column(name = "rule_name", nullable = false, length = 100) private String ruleName;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "issue_id") private IssueEntity issue;
    @Column(name = "issue_key", nullable = false, length = 32) private String issueKey;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private AutomationOutcome outcome;
    @Column(nullable = false, length = 300) private String detail;
    @CreationTimestamp @Column(name = "executed_at", nullable = false, updatable = false) private Instant executedAt;

    protected AutomationExecutionEntity() { }
    public AutomationExecutionEntity(AutomationRuleEntity rule, IssueEntity issue, AutomationOutcome outcome, String detail) {
        this.project = rule.getProject(); this.rule = rule; this.ruleName = rule.getName(); this.issue = issue;
        this.issueKey = issue.getProject().getKey() + "-" + issue.getIssueNumber(); this.outcome = outcome; this.detail = detail;
    }
    public UUID getId() { return id; }
    public UUID getRuleId() { return rule == null ? null : rule.getId(); }
    public String getRuleName() { return ruleName; }
    public String getIssueKey() { return issueKey; }
    public AutomationOutcome getOutcome() { return outcome; }
    public String getDetail() { return detail; }
    public Instant getExecutedAt() { return executedAt; }
}
