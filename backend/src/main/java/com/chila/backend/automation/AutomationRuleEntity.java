package com.chila.backend.automation;

import java.time.Instant;
import java.util.UUID;
import com.chila.backend.issue.IssueStatus;
import com.chila.backend.project.ProjectEntity;
import com.chila.backend.user.UserEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "automation_rules", indexes = @Index(name = "ix_automation_rules_trigger", columnList = "project_id, enabled, target_status, created_at"))
public class AutomationRuleEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false) private ProjectEntity project;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false) private UserEntity createdBy;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false) private boolean enabled;
    @Enumerated(EnumType.STRING) @Column(name = "target_status", nullable = false, length = 24) private IssueStatus targetStatus;
    @Enumerated(EnumType.STRING) @Column(name = "action_type", nullable = false, length = 32) private AutomationAction action;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected AutomationRuleEntity() { }
    public AutomationRuleEntity(ProjectEntity project, UserEntity createdBy, String name, boolean enabled, IssueStatus targetStatus, AutomationAction action) {
        this.project = project; this.createdBy = createdBy; update(name, enabled, targetStatus, action);
    }
    public void update(String name, boolean enabled, IssueStatus targetStatus, AutomationAction action) {
        this.name = name; this.enabled = enabled; this.targetStatus = targetStatus; this.action = action;
    }
    public UUID getId() { return id; }
    public ProjectEntity getProject() { return project; }
    public String getName() { return name; }
    public boolean isEnabled() { return enabled; }
    public IssueStatus getTargetStatus() { return targetStatus; }
    public AutomationAction getAction() { return action; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
