package com.chila.backend.issue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.chila.backend.project.ProjectEntity;
import com.chila.backend.user.UserEntity;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "issues", indexes = {
        @Index(name = "ix_issues_project_status_created", columnList = "project_id, status, created_at"),
        @Index(name = "ix_issues_project_assignee", columnList = "project_id, assignee_id"),
        @Index(name = "ix_issues_project_due_date", columnList = "project_id, due_date")
})
public class IssueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserEntity reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private UserEntity assignee;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 10000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IssueType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssueStatus status = IssueStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IssuePriority priority = IssuePriority.MEDIUM;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IssueEntity() {
    }

    public IssueEntity(ProjectEntity project, UserEntity reporter, String title, String description,
            IssueType type, IssueStatus status, IssuePriority priority, UserEntity assignee, LocalDate dueDate) {
        this.project = project;
        this.reporter = reporter;
        this.title = title;
        this.description = description;
        this.type = type;
        this.status = status;
        this.priority = priority;
        this.assignee = assignee;
        this.dueDate = dueDate;
    }

    public UUID getId() { return id; }
    public ProjectEntity getProject() { return project; }
    public UserEntity getReporter() { return reporter; }
    public UserEntity getAssignee() { return assignee; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public IssueType getType() { return type; }
    public IssueStatus getStatus() { return status; }
    public IssuePriority getPriority() { return priority; }
    public LocalDate getDueDate() { return dueDate; }
    public Instant getArchivedAt() { return archivedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(String title, String description, IssueType type, IssueStatus status,
            IssuePriority priority, UserEntity assignee, LocalDate dueDate) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.status = status;
        this.priority = priority;
        this.assignee = assignee;
        this.dueDate = dueDate;
    }

    public void archive() {
        if (archivedAt == null) archivedAt = Instant.now();
    }

    public void restore() {
        archivedAt = null;
    }
}
