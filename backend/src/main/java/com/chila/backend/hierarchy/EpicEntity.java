package com.chila.backend.hierarchy;

import java.time.Instant;
import java.util.UUID;
import com.chila.backend.project.ProjectEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "epics", uniqueConstraints = @UniqueConstraint(name = "uq_epics_project_name", columnNames = {"project_id", "name"}))
public class EpicEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false) private ProjectEntity project;
    @Column(nullable = false, length = 120) private String name;
    @Column(length = 2000) private String summary;
    @Column(nullable = false, length = 7) private String color = "#6554C0";
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected EpicEntity() { }
    public EpicEntity(ProjectEntity project, String name, String summary, String color) { this.project = project; update(name, summary, color); }
    public UUID getId() { return id; }
    public ProjectEntity getProject() { return project; }
    public String getName() { return name; }
    public String getSummary() { return summary; }
    public String getColor() { return color; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void update(String name, String summary, String color) { this.name = name; this.summary = summary; this.color = color; }
}
