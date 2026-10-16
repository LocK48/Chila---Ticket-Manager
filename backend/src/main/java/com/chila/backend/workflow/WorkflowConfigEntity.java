package com.chila.backend.workflow;

import java.util.UUID;
import com.chila.backend.project.ProjectEntity;
import jakarta.persistence.*;

@Entity @Table(name="workflow_configs")
public class WorkflowConfigEntity {
    @Id @Column(name="project_id") private UUID projectId;
    @MapsId @OneToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="project_id") private ProjectEntity project;
    protected WorkflowConfigEntity() {}
    public WorkflowConfigEntity(ProjectEntity project) { this.project=project; }
}
