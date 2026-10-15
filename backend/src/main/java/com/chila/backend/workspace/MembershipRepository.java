package com.chila.backend.workspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipRepository extends JpaRepository<MembershipEntity, MembershipId> {

    Optional<MembershipEntity> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    List<MembershipEntity> findByWorkspaceIdOrderByJoinedAtAsc(UUID workspaceId);

    void deleteAllByWorkspaceId(UUID workspaceId);

    @Query("select m from MembershipEntity m join fetch m.workspace where m.user.id = :userId order by m.workspace.name")
    List<MembershipEntity> findAllForUser(@Param("userId") UUID userId);
}
