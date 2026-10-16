package com.chila.backend.automation;

import java.util.List;
import java.util.UUID;
import com.chila.backend.issue.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutomationRuleRepository extends JpaRepository<AutomationRuleEntity, UUID> {
    List<AutomationRuleEntity> findByProject_IdOrderByCreatedAtDesc(UUID projectId);
    List<AutomationRuleEntity> findByProject_IdAndEnabledTrueAndTargetStatusOrderByCreatedAtAsc(UUID projectId, IssueStatus targetStatus);
    long countByProject_Id(UUID projectId);
    java.util.Optional<AutomationRuleEntity> findByIdAndProject_Id(UUID id, UUID projectId);
}
