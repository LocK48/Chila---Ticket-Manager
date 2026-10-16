package com.chila.backend.workflow;

import java.util.UUID;
import com.chila.backend.issue.IssueStatus;
import com.chila.backend.project.ProjectEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "workflow_transitions", uniqueConstraints = @UniqueConstraint(name = "uq_workflow_transition", columnNames = {"project_id", "from_status", "to_status"}))
public class WorkflowTransitionEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false) private ProjectEntity project;
    @Enumerated(EnumType.STRING) @Column(name = "from_status", nullable = false, length = 24) private IssueStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name = "to_status", nullable = false, length = 24) private IssueStatus toStatus;
    protected WorkflowTransitionEntity() {}
    public WorkflowTransitionEntity(ProjectEntity project, IssueStatus fromStatus, IssueStatus toStatus) { this.project = project; this.fromStatus = fromStatus; this.toStatus = toStatus; }
    public UUID getId() { return id; }
    public IssueStatus getFromStatus() { return fromStatus; }
    public IssueStatus getToStatus() { return toStatus; }
}
