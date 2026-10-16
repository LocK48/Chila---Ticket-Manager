package com.chila.backend.hierarchy;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EpicRepository extends JpaRepository<EpicEntity, UUID> {
    List<EpicEntity> findByProject_IdOrderByNameAsc(UUID projectId);
    Optional<EpicEntity> findByIdAndProject_Id(UUID id, UUID projectId);
    boolean existsByProject_IdAndNameIgnoreCase(UUID projectId, String name);
}
