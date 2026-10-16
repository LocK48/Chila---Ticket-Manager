package com.chila.backend.sprint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.chila.backend.project.ProjectEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "sprints")
public class SprintEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 2000)
    private String goal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SprintState state = SprintState.FUTURE;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SprintEntity() { }

    public SprintEntity(ProjectEntity project, String name, String goal, LocalDate startDate, LocalDate endDate) {
        this.project = project;
        this.name = name;
        this.goal = goal;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public UUID getId() { return id; }
    public ProjectEntity getProject() { return project; }
    public String getName() { return name; }
    public String getGoal() { return goal; }
    public SprintState getState() { return state; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(String name, String goal, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.goal = goal;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void start() { state = SprintState.ACTIVE; }
    public void close() { state = SprintState.CLOSED; }
}
