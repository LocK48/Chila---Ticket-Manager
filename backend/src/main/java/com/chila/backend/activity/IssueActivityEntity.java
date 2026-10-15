package com.chila.backend.activity;

import java.time.Instant;
import java.util.UUID;

import com.chila.backend.issue.IssueEntity;
import com.chila.backend.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "issue_activities")
public class IssueActivityEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "issue_id", nullable = false)
    private IssueEntity issue;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "actor_id", nullable = false)
    private UserEntity actor;
    @Column(nullable = false, length = 40) private String action;
    @Column(name = "field_name", length = 40) private String fieldName;
    @Column(name = "old_value", length = 2000) private String oldValue;
    @Column(name = "new_value", length = 2000) private String newValue;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected IssueActivityEntity() { }
    public IssueActivityEntity(IssueEntity issue, UserEntity actor, String action, String fieldName, String oldValue, String newValue) {
        this.issue = issue; this.actor = actor; this.action = action; this.fieldName = fieldName;
        this.oldValue = oldValue; this.newValue = newValue;
    }
    public UUID getId() { return id; }
    public IssueEntity getIssue() { return issue; }
    public UserEntity getActor() { return actor; }
    public String getAction() { return action; }
    public String getFieldName() { return fieldName; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public Instant getCreatedAt() { return createdAt; }
}
