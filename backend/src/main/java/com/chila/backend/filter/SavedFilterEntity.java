package com.chila.backend.filter;

import java.time.Instant;
import java.util.UUID;
import com.chila.backend.issue.*;
import com.chila.backend.project.ProjectEntity;
import com.chila.backend.user.UserEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "saved_filters", indexes = {@Index(name="ix_saved_filters_project_visibility", columnList="project_id, visibility"), @Index(name="ix_saved_filters_creator", columnList="created_by")})
public class SavedFilterEntity {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="project_id", nullable=false) private ProjectEntity project;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="created_by", nullable=false) private UserEntity creator;
    @Column(nullable=false, length=100) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=16) private SavedFilterVisibility visibility;
    @Enumerated(EnumType.STRING) @Column(length=24) private IssueStatus status;
    @Enumerated(EnumType.STRING) @Column(length=24) private IssuePriority priority;
    @Enumerated(EnumType.STRING) @Column(length=24) private IssueType type;
    @Column(name="assignee_id") private UUID assigneeId;
    @Column(nullable=false) private boolean unassigned;
    @Column(name="sprint_id") private UUID sprintId;
    @Column(name="epic_id") private UUID epicId;
    @Column(name="due_from") private java.time.LocalDate dueFrom;
    @Column(name="due_to") private java.time.LocalDate dueTo;
    @Column(length=200) private String search;
    @CreationTimestamp @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;
    @UpdateTimestamp @Column(name="updated_at", nullable=false) private Instant updatedAt;
    protected SavedFilterEntity() {}
    public SavedFilterEntity(ProjectEntity project, UserEntity creator, String name, SavedFilterVisibility visibility, IssueStatus status, IssuePriority priority, IssueType type, UUID assigneeId, boolean unassigned, UUID sprintId, UUID epicId, java.time.LocalDate dueFrom, java.time.LocalDate dueTo, String search) { this.project=project; this.creator=creator; update(name,visibility,status,priority,type,assigneeId,unassigned,sprintId,epicId,dueFrom,dueTo,search); }
    public void update(String name,SavedFilterVisibility visibility,IssueStatus status,IssuePriority priority,IssueType type,UUID assigneeId,boolean unassigned,UUID sprintId,UUID epicId,java.time.LocalDate dueFrom,java.time.LocalDate dueTo,String search) { this.name=name;this.visibility=visibility;this.status=status;this.priority=priority;this.type=type;this.assigneeId=assigneeId;this.unassigned=unassigned;this.sprintId=sprintId;this.epicId=epicId;this.dueFrom=dueFrom;this.dueTo=dueTo;this.search=search; }
    public UUID getId(){return id;} public UUID getProjectId(){return project.getId();} public UUID getCreatorId(){return creator.getId();} public String getName(){return name;} public SavedFilterVisibility getVisibility(){return visibility;} public IssueStatus getStatus(){return status;} public IssuePriority getPriority(){return priority;} public IssueType getType(){return type;} public UUID getAssigneeId(){return assigneeId;} public boolean isUnassigned(){return unassigned;} public UUID getSprintId(){return sprintId;} public UUID getEpicId(){return epicId;} public java.time.LocalDate getDueFrom(){return dueFrom;} public java.time.LocalDate getDueTo(){return dueTo;} public String getSearch(){return search;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
