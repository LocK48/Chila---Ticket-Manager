package com.chila.backend.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {

    @Query("select p from ProjectEntity p join fetch p.workspace join fetch p.createdBy "
            + "where p.workspace.id = :workspaceId order by p.name")
    List<ProjectEntity> findAllForWorkspace(@Param("workspaceId") UUID workspaceId);

    boolean existsByWorkspace_IdAndKey(UUID workspaceId, String key);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProjectEntity p where p.id = :projectId")
    Optional<ProjectEntity> findByIdForUpdate(@Param("projectId") UUID projectId);
}
