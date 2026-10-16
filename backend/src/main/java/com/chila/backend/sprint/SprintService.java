package com.chila.backend.sprint;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.chila.backend.common.api.InvalidInputException;
import com.chila.backend.issue.IssueEntity;
import com.chila.backend.issue.IssueRepository;
import com.chila.backend.issue.IssueStatus;
import com.chila.backend.issue.IssuePriority;
import com.chila.backend.issue.IssueType;
import com.chila.backend.issue.api.IssuePageResponse;
import com.chila.backend.issue.api.IssueResponse;
import com.chila.backend.project.ProjectEntity;
import com.chila.backend.project.ProjectNotFoundException;
import com.chila.backend.project.ProjectRepository;
import com.chila.backend.project.ProjectStatus;
import com.chila.backend.sprint.api.CompleteSprintRequest;
import com.chila.backend.sprint.api.SprintRequest;
import com.chila.backend.sprint.api.SprintResponse;
import com.chila.backend.sprint.api.SprintIssueSelectionRequest;
import com.chila.backend.user.UserEntity;
import com.chila.backend.workspace.MembershipEntity;
import com.chila.backend.workspace.MembershipRepository;
import com.chila.backend.workspace.MembershipRole;
import com.chila.backend.workspace.WorkspaceAccessDeniedException;
import com.chila.backend.workspace.WorkspaceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SprintService {
    private static final long RANK_STEP = 1024;
    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final MembershipRepository membershipRepository;
    private final IssueRepository issueRepository;

    public SprintService(SprintRepository sprintRepository, ProjectRepository projectRepository,
            MembershipRepository membershipRepository, IssueRepository issueRepository) {
        this.sprintRepository = sprintRepository;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.issueRepository = issueRepository;
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> list(UUID actorId, UUID projectId) {
        ProjectEntity project = project(projectId);
        requireMember(project, actorId);
        return sprintRepository.findByProject_IdOrderByCreatedAtDesc(projectId).stream()
                .map(SprintService::response).toList();
    }

    @Transactional
    public SprintResponse create(UUID actorId, UUID projectId, SprintRequest request) {
        ProjectEntity project = projectRepository.findByIdForUpdate(projectId).orElseThrow(ProjectNotFoundException::new);
        requireManager(project, actorId);
        requireActive(project);
        validateDates(request.startDate(), request.endDate());
        return response(sprintRepository.save(new SprintEntity(project, request.name().trim(), normalize(request.goal()),
                request.startDate(), request.endDate())));
    }

    @Transactional
    public SprintResponse update(UUID actorId, UUID projectId, UUID sprintId, SprintRequest request) {
        ProjectEntity project = projectRepository.findByIdForUpdate(projectId).orElseThrow(ProjectNotFoundException::new);
        requireManager(project, actorId);
        requireActive(project);
        SprintEntity sprint = sprintRepository.findByIdAndProject_Id(sprintId, projectId).orElseThrow(SprintNotFoundException::new);
        if (sprint.getState() != SprintState.FUTURE) {
            throw new SprintConflictException("Only a future sprint can be edited.");
        }
        validateDates(request.startDate(), request.endDate());
        sprint.update(request.name().trim(), normalize(request.goal()), request.startDate(), request.endDate());
        return response(sprint);
    }

    @Transactional
    public SprintResponse start(UUID actorId, UUID projectId, UUID sprintId) {
        ProjectEntity project = projectRepository.findByIdForUpdate(projectId).orElseThrow(ProjectNotFoundException::new);
        requireManager(project, actorId);
        requireActive(project);
        SprintEntity sprint = sprintRepository.findByIdAndProject_Id(sprintId, projectId).orElseThrow(SprintNotFoundException::new);
        if (sprint.getState() != SprintState.FUTURE) throw new SprintConflictException("Only a future sprint can be started.");
        if (sprintRepository.existsByProject_IdAndState(projectId, SprintState.ACTIVE)) {
            throw new SprintConflictException("This project already has an active sprint.");
        }
        LocalDate start = sprint.getStartDate() == null ? LocalDate.now() : sprint.getStartDate();
        LocalDate end = sprint.getEndDate();
        if (end == null || end.isBefore(start)) {
            throw new InvalidInputException("endDate", "Set an end date on or after the sprint start date before starting it.");
        }
        if (start.isAfter(LocalDate.now())) {
            throw new InvalidInputException("startDate", "A sprint cannot start before its planned start date.");
        }
        sprint.update(sprint.getName(), sprint.getGoal(), start, end);
        sprint.start();
        return response(sprint);
    }

    @Transactional
    public SprintResponse complete(UUID actorId, UUID projectId, UUID sprintId, CompleteSprintRequest request) {
        ProjectEntity project = projectRepository.findByIdForUpdate(projectId).orElseThrow(ProjectNotFoundException::new);
        requireManager(project, actorId);
        requireActive(project);
        SprintEntity sprint = sprintRepository.findByIdAndProject_Id(sprintId, projectId).orElseThrow(SprintNotFoundException::new);
        if (sprint.getState() != SprintState.ACTIVE) throw new SprintConflictException("Only an active sprint can be completed.");

        SprintEntity target = null;
        if (request.unfinishedIssueAction() == CompleteSprintRequest.UnfinishedIssueAction.FUTURE_SPRINT) {
            if (request.targetSprintId() == null || request.targetSprintId().equals(sprintId)) {
                throw new InvalidInputException("targetSprintId", "Choose a different future sprint for unfinished issues.");
            }
            target = sprintRepository.findByIdAndProject_Id(request.targetSprintId(), projectId)
                    .orElseThrow(SprintNotFoundException::new);
            if (target.getState() != SprintState.FUTURE) {
                throw new SprintConflictException("Unfinished issues can only move to a future sprint.");
            }
        } else if (request.targetSprintId() != null) {
            throw new InvalidInputException("targetSprintId", "Do not choose a target sprint when returning issues to the backlog.");
        }

        long targetRank = target == null ? issueRepository.findMaxBacklogRank(projectId)
                : issueRepository.findMaxBacklogRankInSprint(projectId, target.getId());
        for (IssueEntity issue : issueRepository.findByProject_IdAndSprint_IdAndArchivedAtIsNull(projectId, sprintId)) {
            if (issue.getStatus() == com.chila.backend.issue.IssueStatus.DONE) continue;
            if (request.unfinishedIssueAction() == CompleteSprintRequest.UnfinishedIssueAction.BACKLOG) {
                issue.assignToSprint(null);
                issue.setBacklogRank(Math.addExact(targetRank, RANK_STEP));
                targetRank = issue.getBacklogRank();
            } else {
                issue.assignToSprint(target);
                issue.setBacklogRank(Math.addExact(targetRank, RANK_STEP));
                targetRank = issue.getBacklogRank();
            }
        }
        sprint.close();
        return response(sprint);
    }

    @Transactional
    public void addIssues(UUID actorId, UUID projectId, UUID sprintId, SprintIssueSelectionRequest request) {
        ProjectEntity project = projectRepository.findByIdForUpdate(projectId).orElseThrow(ProjectNotFoundException::new);
        requireMember(project, actorId);
        requireActive(project);
        SprintEntity sprint = sprintRepository.findByIdAndProject_Id(sprintId, projectId).orElseThrow(SprintNotFoundException::new);
        if (sprint.getState() == SprintState.CLOSED) throw new SprintConflictException("Issues cannot be added to a closed sprint.");
        List<IssueEntity> issues = issueRepository.findByIdInAndProject_Id(request.issueIds().stream().distinct().toList(), projectId);
        if (issues.size() != request.issueIds().stream().distinct().count()) {
            throw new InvalidInputException("issueIds", "Every selected issue must belong to this project.");
        }
        long rank = issueRepository.findMaxBacklogRankInSprint(projectId, sprintId);
        for (IssueEntity issue : issues) {
            if (issue.getArchivedAt() != null) throw new SprintConflictException("Archived issues cannot be planned into a sprint.");
            if (issue.getSprint() != null && !issue.getSprint().getId().equals(sprintId)) {
                if (issue.getSprint().getState() == SprintState.CLOSED) {
                    throw new SprintConflictException("Issues in closed sprints cannot be reassigned.");
                }
                throw new InvalidInputException("issueIds", "Return issues to the backlog before planning another sprint.");
            }
            issue.assignToSprint(sprint);
            issue.setBacklogRank(Math.addExact(rank, RANK_STEP));
            rank = issue.getBacklogRank();
        }
    }

    @Transactional
    public void removeIssues(UUID actorId, UUID projectId, UUID sprintId, SprintIssueSelectionRequest request) {
        ProjectEntity project = projectRepository.findByIdForUpdate(projectId).orElseThrow(ProjectNotFoundException::new);
        requireMember(project, actorId);
        requireActive(project);
        SprintEntity sprint = sprintRepository.findByIdAndProject_Id(sprintId, projectId).orElseThrow(SprintNotFoundException::new);
        if (sprint.getState() == SprintState.CLOSED) throw new SprintConflictException("Issues from a closed sprint remain in sprint history.");
        List<IssueEntity> issues = issueRepository.findByIdInAndProject_Id(request.issueIds().stream().distinct().toList(), projectId);
        if (issues.size() != request.issueIds().stream().distinct().count()
                || issues.stream().anyMatch(issue -> issue.getSprint() == null || !issue.getSprint().getId().equals(sprintId))) {
            throw new InvalidInputException("issueIds", "Every selected issue must belong to this sprint.");
        }
        long rank = issueRepository.findMaxBacklogRank(projectId);
        for (IssueEntity issue : issues) {
            issue.assignToSprint(null);
            issue.setBacklogRank(Math.addExact(rank, RANK_STEP));
            rank = issue.getBacklogRank();
        }
    }

    @Transactional(readOnly = true)
    public IssuePageResponse issues(UUID actorId, UUID projectId, UUID sprintId, IssueStatus status,
            IssuePriority priority, IssueType type, UUID assigneeId, boolean unassigned, String search, int page, int size) {
        ProjectEntity project = project(projectId);
        requireMember(project, actorId);
        SprintEntity sprint = sprintRepository.findByIdAndProject_Id(sprintId, projectId).orElseThrow(SprintNotFoundException::new);
        if (page < 0 || size < 1 || size > 100) throw new InvalidInputException("page", "Invalid page or size.");
        if (unassigned && assigneeId != null) throw new InvalidInputException("assigneeId", "Choose a member or unassigned issues, not both.");
        var results = issueRepository.searchSprint(projectId, sprint.getId(), status, priority, type, assigneeId, unassigned,
                StringUtils.hasText(search) ? "%" + search.trim().toLowerCase() + "%" : "%",
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "rankValue").and(Sort.by("id"))));
        List<IssueResponse> items = results.getContent().stream()
                .map(issue -> new IssueResponse(issue.getId(), projectId, sprintId, project.getKey(),
                        project.getKey() + "-" + issue.getIssueNumber(), issue.getIssueNumber(), issue.getTitle(),
                        issue.getDescription(), issue.getType(), issue.getStatus(), issue.getPriority(),
                        issue.getReporter().getId(), issue.getReporter().getDisplayName(),
                        issue.getAssignee() == null ? null : issue.getAssignee().getId(),
                        issue.getAssignee() == null ? null : issue.getAssignee().getDisplayName(), issue.getDueDate(),
                        issue.getArchivedAt(), issue.getCreatedAt(), issue.getUpdatedAt()))
                .toList();
        return new IssuePageResponse(items, results.getNumber(), results.getSize(), results.getTotalElements(), results.getTotalPages());
    }

    private ProjectEntity project(UUID id) { return projectRepository.findById(id).orElseThrow(ProjectNotFoundException::new); }

    private void requireMember(ProjectEntity project, UUID userId) {
        membershipRepository.findByWorkspaceIdAndUserId(project.getWorkspace().getId(), userId)
                .orElseThrow(WorkspaceNotFoundException::new);
    }

    private void requireManager(ProjectEntity project, UUID userId) {
        MembershipEntity member = membershipRepository.findByWorkspaceIdAndUserId(project.getWorkspace().getId(), userId)
                .orElseThrow(WorkspaceNotFoundException::new);
        if (member.getRole() == MembershipRole.MEMBER) throw new WorkspaceAccessDeniedException();
    }

    private static void requireActive(ProjectEntity project) {
        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new SprintConflictException("Sprints cannot be changed in an archived project.");
        }
    }

    private static void validateDates(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new InvalidInputException("endDate", "Sprint end date must be on or after its start date.");
        }
    }

    private static String normalize(String value) { return StringUtils.hasText(value) ? value.trim() : null; }

    private static SprintResponse response(SprintEntity sprint) {
        return new SprintResponse(sprint.getId(), sprint.getProject().getId(), sprint.getName(), sprint.getGoal(),
                sprint.getState(), sprint.getStartDate(), sprint.getEndDate(), sprint.getCreatedAt(), sprint.getUpdatedAt());
    }
}
