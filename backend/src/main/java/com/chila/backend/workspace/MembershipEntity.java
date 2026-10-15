package com.chila.backend.workspace;

import java.time.Instant;

import com.chila.backend.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "memberships")
public class MembershipEntity {

    @EmbeddedId
    private MembershipId id;

    @MapsId("workspaceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceEntity workspace;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MembershipRole role;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected MembershipEntity() {
    }

    public MembershipEntity(WorkspaceEntity workspace, UserEntity user, MembershipRole role) {
        this.id = new MembershipId(workspace.getId(), user.getId());
        this.workspace = workspace;
        this.user = user;
        this.role = role;
    }

    public MembershipId getId() { return id; }
    public WorkspaceEntity getWorkspace() { return workspace; }
    public UserEntity getUser() { return user; }
    public MembershipRole getRole() { return role; }
    public Instant getJoinedAt() { return joinedAt; }
    public void setRole(MembershipRole role) { this.role = role; }
}
