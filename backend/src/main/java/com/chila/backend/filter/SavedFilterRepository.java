package com.chila.backend.filter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedFilterRepository extends JpaRepository<SavedFilterEntity, UUID> {
    List<SavedFilterEntity> findByProject_IdAndVisibilityOrderByNameAsc(UUID projectId, SavedFilterVisibility visibility);
    List<SavedFilterEntity> findByProject_IdAndCreator_IdOrderByNameAsc(UUID projectId, UUID creatorId);
    Optional<SavedFilterEntity> findByIdAndProject_Id(UUID id, UUID projectId);
}
