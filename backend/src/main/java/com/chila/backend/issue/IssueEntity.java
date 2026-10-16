package com.chila.backend.issue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.chila.backend.project.ProjectEntity;
import com.chila.backend.sprint.SprintEntity;
import com.chila.backend.hierarchy.EpicEntity;
import com.chila.backend.hierarchy.ReleaseEntity;
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

    @Column(name = "issue_number", nullable = false, updatable = false)
    private long issueNumber;

    @Column(name = "rank_value", nullable = false)
    private long rankValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private SprintEntity sprint;

    @Column(name = "backlog_rank", nullable = false)
    private long backlogRank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "epic_id")
    private EpicEntity epic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_issue_id")
    private IssueEntity parentIssue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_id")
    private ReleaseEntity release;

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
            IssueType type, IssueStatus status, IssuePriority priority, UserEntity assignee, LocalDate dueDate,
            long issueNumber, long rankValue, long backlogRank) {
        this.project = project;
        this.reporter = reporter;
        this.title = title;
        this.description = description;
        this.type = type;
        this.status = status;
        this.priority = priority;
        this.assignee = assignee;
        this.dueDate = dueDate;
        this.issueNumber = issueNumber;
        this.rankValue = rankValue;
        this.backlogRank = backlogRank;
    }

    public UUID getId() { return id; }
    public ProjectEntity getProject() { return project; }
    public UserEntity getReporter() { return reporter; }
    public UserEntity getAssignee() { return assignee; }
    public long getIssueNumber() { return issueNumber; }
    public long getRankValue() { return rankValue; }
    public SprintEntity getSprint() { return sprint; }
    public long getBacklogRank() { return backlogRank; }
    public EpicEntity getEpic() { return epic; }
    public IssueEntity getParentIssue() { return parentIssue; }
    public ReleaseEntity getRelease() { return release; }
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

    public void setRankValue(long rankValue) {
        this.rankValue = rankValue;
    }

    public void moveTo(IssueStatus status, long rankValue) {
        this.status = status;
        this.rankValue = rankValue;
    }

    public void assignToSprint(SprintEntity sprint) { this.sprint = sprint; }
    public void setBacklogRank(long backlogRank) { this.backlogRank = backlogRank; }
    public void assignEpic(EpicEntity epic) { this.epic = epic; }
    public void setParentIssue(IssueEntity parentIssue) { this.parentIssue = parentIssue; }
    public void assignRelease(ReleaseEntity release) { this.release = release; }
}
