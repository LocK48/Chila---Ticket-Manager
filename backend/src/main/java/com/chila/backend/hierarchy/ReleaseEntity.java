package com.chila.backend.hierarchy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import com.chila.backend.project.ProjectEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "releases", uniqueConstraints = @UniqueConstraint(name = "uq_releases_project_name", columnNames = {"project_id", "name"}))
public class ReleaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false) private ProjectEntity project;
    @Column(nullable = false, length = 120) private String name;
    @Column(length = 2000) private String description;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "release_date") private LocalDate releaseDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private ReleaseState state = ReleaseState.UNRELEASED;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected ReleaseEntity() { }
    public ReleaseEntity(ProjectEntity project, String name, String description, LocalDate startDate, LocalDate releaseDate) {
        this.project = project; update(name, description, startDate, releaseDate);
    }
    public UUID getId() { return id; }
    public ProjectEntity getProject() { return project; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public ReleaseState getState() { return state; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void update(String name, String description, LocalDate startDate, LocalDate releaseDate) {
        this.name = name; this.description = description; this.startDate = startDate; this.releaseDate = releaseDate;
    }
    public void setState(ReleaseState state) { this.state = state; }
}
