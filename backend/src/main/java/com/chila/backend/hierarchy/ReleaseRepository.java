package com.chila.backend.hierarchy;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReleaseRepository extends JpaRepository<ReleaseEntity, UUID> {
    List<ReleaseEntity> findByProject_IdOrderByStartDateAscReleaseDateAscNameAsc(UUID projectId);
    Optional<ReleaseEntity> findByIdAndProject_Id(UUID id, UUID projectId);
    boolean existsByProject_IdAndNameIgnoreCase(UUID projectId, String name);
}
