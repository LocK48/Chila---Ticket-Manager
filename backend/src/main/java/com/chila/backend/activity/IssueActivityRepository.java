package com.chila.backend.activity;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueActivityRepository extends JpaRepository<IssueActivityEntity, UUID> {
    Page<IssueActivityEntity> findByIssueIdOrderByCreatedAtDesc(UUID issueId, Pageable pageable);
}
