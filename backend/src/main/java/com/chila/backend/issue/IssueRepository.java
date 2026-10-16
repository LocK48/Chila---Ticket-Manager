package com.chila.backend.issue;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

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
            + "or lower(coalesce(i.description, '')) like :search "
            + "or lower(concat(concat(p.key, '-'), cast(i.issueNumber as string))) like :search) "
            + "and (:includeArchived = true or i.archivedAt is null)",
            countQuery = "select count(i) from IssueEntity i join i.project p where p.id = :projectId "
                    + "and (:status is null or i.status = :status) "
                    + "and (:priority is null or i.priority = :priority) "
                    + "and (:type is null or i.type = :type) "
                    + "and (:assigneeId is null or i.assignee.id = :assigneeId) "
                    + "and (:unassigned = false or i.assignee is null) "
                    + "and (:search = '%' or lower(i.title) like :search "
                    + "or lower(coalesce(i.description, '')) like :search "
                    + "or lower(concat(concat(p.key, '-'), cast(i.issueNumber as string))) like :search) "
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

    @Query("select i from IssueEntity i join fetch i.project p join fetch p.workspace "
            + "join fetch i.reporter left join fetch i.assignee "
            + "where p.workspace.id = :workspaceId and p.key = :projectKey and i.issueNumber = :issueNumber")
    Optional<IssueEntity> findForWorkspaceByKey(
            @Param("workspaceId") UUID workspaceId,
            @Param("projectKey") String projectKey,
            @Param("issueNumber") long issueNumber);

    List<IssueEntity> findByProject_IdAndStatusAndArchivedAtIsNullOrderByRankValueAscIdAsc(
            UUID projectId, IssueStatus status);

    @Query("select coalesce(max(i.rankValue), 0) from IssueEntity i "
            + "where i.project.id = :projectId and i.status = :status and i.archivedAt is null")
    long findMaxActiveRank(@Param("projectId") UUID projectId, @Param("status") IssueStatus status);

    @Query("select coalesce(max(i.backlogRank), 0) from IssueEntity i "
            + "where i.project.id = :projectId and i.sprint is null and i.archivedAt is null")
    long findMaxBacklogRank(@Param("projectId") UUID projectId);

    @Query("select coalesce(max(i.backlogRank), 0) from IssueEntity i "
            + "where i.project.id = :projectId and i.sprint is null and i.archivedAt is null and i.id <> :issueId")
    long findMaxBacklogRankExcluding(@Param("projectId") UUID projectId, @Param("issueId") UUID issueId);

    @Query("select i from IssueEntity i where i.id = :issueId and i.project.id = :projectId "
            + "and i.sprint is null and i.archivedAt is null")
    Optional<IssueEntity> findBacklogIssue(@Param("projectId") UUID projectId, @Param("issueId") UUID issueId);

    @Query("select i from IssueEntity i where i.project.id = :projectId and i.sprint is null "
            + "and i.archivedAt is null and i.id <> :issueId and i.backlogRank < :rank order by i.backlogRank desc, i.id desc")
    List<IssueEntity> findPreviousBacklogIssue(@Param("projectId") UUID projectId, @Param("issueId") UUID issueId,
            @Param("rank") long rank, Pageable pageable);

    List<IssueEntity> findByProject_IdAndSprintIsNullAndArchivedAtIsNullOrderByBacklogRankAscIdAsc(UUID projectId);

    @Query(value = "select i from IssueEntity i join fetch i.project p join fetch p.workspace "
            + "join fetch i.reporter left join fetch i.assignee "
            + "where p.id = :projectId and i.sprint is null and i.archivedAt is null "
            + "and (:search = '%' or lower(i.title) like :search "
            + "or lower(concat(concat(p.key, '-'), cast(i.issueNumber as string))) like :search) "
            + "and (:priority is null or i.priority = :priority) and (:type is null or i.type = :type) "
            + "and (:assigneeId is null or i.assignee.id = :assigneeId) "
            + "and (:unassigned = false or i.assignee is null)",
            countQuery = "select count(i) from IssueEntity i join i.project p "
                    + "where p.id = :projectId and i.sprint is null and i.archivedAt is null "
                    + "and (:search = '%' or lower(i.title) like :search "
                    + "or lower(concat(concat(p.key, '-'), cast(i.issueNumber as string))) like :search) "
                    + "and (:priority is null or i.priority = :priority) and (:type is null or i.type = :type) "
                    + "and (:assigneeId is null or i.assignee.id = :assigneeId) "
                    + "and (:unassigned = false or i.assignee is null)")
    Page<IssueEntity> searchBacklog(@Param("projectId") UUID projectId,
            @Param("search") String search, @Param("priority") IssuePriority priority,
            @Param("type") IssueType type, @Param("assigneeId") UUID assigneeId,
            @Param("unassigned") boolean unassigned, Pageable pageable);

    @Query(value = "select i from IssueEntity i join fetch i.project p join fetch p.workspace "
            + "join fetch i.reporter left join fetch i.assignee "
            + "where p.id = :projectId and i.sprint.id = :sprintId and i.archivedAt is null "
            + "and (:status is null or i.status = :status) "
            + "and (:priority is null or i.priority = :priority) and (:type is null or i.type = :type) "
            + "and (:assigneeId is null or i.assignee.id = :assigneeId) "
            + "and (:unassigned = false or i.assignee is null) "
            + "and (:search = '%' or lower(i.title) like :search "
            + "or lower(concat(concat(p.key, '-'), cast(i.issueNumber as string))) like :search)",
            countQuery = "select count(i) from IssueEntity i join i.project p where p.id = :projectId "
                    + "and i.sprint.id = :sprintId and i.archivedAt is null and (:status is null or i.status = :status) "
                    + "and (:priority is null or i.priority = :priority) and (:type is null or i.type = :type) "
                    + "and (:assigneeId is null or i.assignee.id = :assigneeId) "
                    + "and (:unassigned = false or i.assignee is null) "
                    + "and (:search = '%' or lower(i.title) like :search "
                    + "or lower(concat(concat(p.key, '-'), cast(i.issueNumber as string))) like :search)")
    Page<IssueEntity> searchSprint(@Param("projectId") UUID projectId, @Param("sprintId") UUID sprintId,
            @Param("status") IssueStatus status, @Param("priority") IssuePriority priority,
            @Param("type") IssueType type, @Param("assigneeId") UUID assigneeId,
            @Param("unassigned") boolean unassigned, @Param("search") String search, Pageable pageable);

    List<IssueEntity> findByProject_IdAndSprint_IdAndArchivedAtIsNull(UUID projectId, UUID sprintId);

    List<IssueEntity> findByIdInAndProject_Id(List<UUID> ids, UUID projectId);

    @Query("select coalesce(max(i.backlogRank), 0) from IssueEntity i "
            + "where i.project.id = :projectId and i.sprint.id = :sprintId")
    long findMaxBacklogRankInSprint(@Param("projectId") UUID projectId, @Param("sprintId") UUID sprintId);
}
