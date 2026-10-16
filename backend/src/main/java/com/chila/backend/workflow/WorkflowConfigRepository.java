package com.chila.backend.workflow;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WorkflowConfigRepository extends JpaRepository<WorkflowConfigEntity, UUID> {}
