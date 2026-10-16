package com.chila.backend.workflow;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransitionEntity, UUID> {
    List<WorkflowTransitionEntity> findByProject_IdOrderByFromStatusAscToStatusAsc(UUID projectId);
    boolean existsByProject_IdAndFromStatusAndToStatus(UUID projectId, com.chila.backend.issue.IssueStatus from, com.chila.backend.issue.IssueStatus to);
    void deleteByProject_Id(UUID projectId);
}
