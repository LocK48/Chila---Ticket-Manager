package com.chila.backend.notification;

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
@Table(name = "notifications")
public class NotificationEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "recipient_id", nullable = false)
    private UserEntity recipient;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "issue_id")
    private IssueEntity issue;
    @Column(name = "event_type", nullable = false, length = 24) private String eventType;
    @Column(nullable = false, length = 500) private String message;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "read_at") private Instant readAt;
    protected NotificationEntity() { }
    public NotificationEntity(UserEntity recipient, IssueEntity issue, String eventType, String message) {
        this.recipient = recipient; this.issue = issue; this.eventType = eventType; this.message = message;
    }
    public UUID getId() { return id; }
    public UserEntity getRecipient() { return recipient; }
    public IssueEntity getIssue() { return issue; }
    public String getEventType() { return eventType; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
    public void markRead() { if (readAt == null) readAt = Instant.now(); }
}
