package com.chila.backend.comment;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueCommentRepository extends JpaRepository<IssueCommentEntity, UUID> {
    Page<IssueCommentEntity> findByIssueIdOrderByCreatedAtDesc(UUID issueId, Pageable pageable);
}
