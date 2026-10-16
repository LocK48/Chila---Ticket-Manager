package com.chila.backend.hierarchy;

import java.time.Instant;
import java.util.UUID;
import com.chila.backend.issue.IssueEntity;
import com.chila.backend.user.UserEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "issue_links", uniqueConstraints = @UniqueConstraint(name = "uq_issue_links_pair_type", columnNames = {"source_issue_id", "target_issue_id", "link_type"}))
public class IssueLinkEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "source_issue_id", nullable = false) private IssueEntity source;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "target_issue_id", nullable = false) private IssueEntity target;
    @Enumerated(EnumType.STRING) @Column(name = "link_type", nullable = false, length = 20) private IssueLinkType type;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false) private UserEntity createdBy;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected IssueLinkEntity() { }
    public IssueLinkEntity(IssueEntity source, IssueEntity target, IssueLinkType type, UserEntity createdBy) {
        this.source = source; this.target = target; this.type = type; this.createdBy = createdBy;
    }
    public UUID getId() { return id; }
    public IssueEntity getSource() { return source; }
    public IssueEntity getTarget() { return target; }
    public IssueLinkType getType() { return type; }
    public UserEntity getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
