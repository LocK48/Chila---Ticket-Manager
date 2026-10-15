package com.chila.backend.project;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.chila.backend.user.UserEntity;
import com.chila.backend.workspace.WorkspaceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "projects",
        uniqueConstraints = @UniqueConstraint(name = "uq_projects_workspace_key", columnNames = {"workspace_id", "project_key"}),
        indexes = @Index(name = "ix_projects_workspace_status_name", columnList = "workspace_id, status, name"))
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceEntity workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "project_key", nullable = false, length = 10)
    private String key;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @Column(name = "next_issue_number", nullable = false)
    private long nextIssueNumber = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProjectEntity() {
    }

    public ProjectEntity(
            WorkspaceEntity workspace, UserEntity createdBy, String name, String key, String description) {
        this.workspace = workspace;
        this.createdBy = createdBy;
        this.name = name;
        this.key = key.toUpperCase(Locale.ROOT);
        this.description = description;
    }

    public UUID getId() { return id; }
    public WorkspaceEntity getWorkspace() { return workspace; }
    public UserEntity getCreatedBy() { return createdBy; }
    public String getName() { return name; }
    public String getKey() { return key; }
    public String getDescription() { return description; }
    public ProjectStatus getStatus() { return status; }
    public long allocateIssueNumber() {
        if (nextIssueNumber == Long.MAX_VALUE) throw new IllegalStateException("Project issue number space is exhausted.");
        return nextIssueNumber++;
    }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void updateDetails(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }
}
