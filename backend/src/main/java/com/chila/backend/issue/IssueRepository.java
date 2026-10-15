package com.chila.backend.issue;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IssueRepository extends JpaRepository<IssueEntity, UUID> {

    @Query(value = "select i from IssueEntity i join fetch i.project p join fetch p.workspace "
            + "join fetch i.reporter left join fetch i.assignee "
            + "where p.id = :projectId "
            + "and (:status is null or i.status = :status) "
            + "and (:priority is null or i.priority = :priority) "
            + "and (:type is null or i.type = :type) "
            + "and (:assigneeId is null or i.assignee.id = :assigneeId) "
            + "and (:unassigned = false or i.assignee is null) "
            + "and (:search = '%' or lower(i.title) like :search "
            + "or lower(coalesce(i.description, '')) like :search) "
            + "and (:includeArchived = true or i.archivedAt is null)",
            countQuery = "select count(i) from IssueEntity i join i.project p where p.id = :projectId "
                    + "and (:status is null or i.status = :status) "
                    + "and (:priority is null or i.priority = :priority) "
                    + "and (:type is null or i.type = :type) "
                    + "and (:assigneeId is null or i.assignee.id = :assigneeId) "
                    + "and (:unassigned = false or i.assignee is null) "
                    + "and (:search = '%' or lower(i.title) like :search "
                    + "or lower(coalesce(i.description, '')) like :search) "
                    + "and (:includeArchived = true or i.archivedAt is null)")
    Page<IssueEntity> search(
            @Param("projectId") UUID projectId,
            @Param("status") IssueStatus status,
            @Param("priority") IssuePriority priority,
            @Param("type") IssueType type,
            @Param("assigneeId") UUID assigneeId,
            @Param("unassigned") boolean unassigned,
            @Param("search") String search,
            @Param("includeArchived") boolean includeArchived,
            Pageable pageable);
}
