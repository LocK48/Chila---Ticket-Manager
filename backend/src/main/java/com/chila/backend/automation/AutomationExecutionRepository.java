package com.chila.backend.automation;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutomationExecutionRepository extends JpaRepository<AutomationExecutionEntity, UUID> {
    List<AutomationExecutionEntity> findByProject_IdOrderByExecutedAtDesc(UUID projectId, PageRequest page);
}
