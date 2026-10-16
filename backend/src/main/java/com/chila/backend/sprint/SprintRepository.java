package com.chila.backend.sprint;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface SprintRepository extends JpaRepository<SprintEntity, UUID> {
    List<SprintEntity> findByProject_IdOrderByCreatedAtDesc(UUID projectId);

    Optional<SprintEntity> findByIdAndProject_Id(UUID sprintId, UUID projectId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SprintEntity s where s.id = :id")
    Optional<SprintEntity> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByProject_IdAndState(UUID projectId, SprintState state);
}
