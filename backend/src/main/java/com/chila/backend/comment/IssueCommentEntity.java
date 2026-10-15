package com.chila.backend.comment;

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
@Table(name = "issue_comments")
public class IssueCommentEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "issue_id", nullable = false)
    private IssueEntity issue;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;
    @Column(nullable = false, length = 5000) private String body;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected IssueCommentEntity() { }
    public IssueCommentEntity(IssueEntity issue, UserEntity author, String body) {
        this.issue = issue; this.author = author; this.body = body;
    }
    public UUID getId() { return id; }
    public IssueEntity getIssue() { return issue; }
    public UserEntity getAuthor() { return author; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}
