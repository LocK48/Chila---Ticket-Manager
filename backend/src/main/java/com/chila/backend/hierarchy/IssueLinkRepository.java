package com.chila.backend.hierarchy;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface IssueLinkRepository extends JpaRepository<IssueLinkEntity, UUID> {
    List<IssueLinkEntity> findBySource_IdOrTarget_IdOrderByCreatedAtDesc(UUID sourceId, UUID targetId);
    boolean existsBySource_IdAndTarget_IdAndType(UUID sourceId, UUID targetId, IssueLinkType type);
}
