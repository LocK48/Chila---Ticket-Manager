package com.chila.backend.issue;

import java.util.UUID;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.chila.backend.activity.IssueActivityService;
import com.chila.backend.common.api.InvalidInputException;
import com.chila.backend.issue.api.CreateIssueRequest;
import com.chila.backend.issue.api.IssuePageResponse;
import com.chila.backend.issue.api.IssueResponse;
import com.chila.backend.issue.api.MoveIssueRequest;
import com.chila.backend.sprint.SprintConflictException;
import com.chila.backend.sprint.SprintEntity;
import com.chila.backend.sprint.SprintNotFoundException;
import com.chila.backend.sprint.SprintRepository;
import com.chila.backend.sprint.SprintState;
import com.chila.backend.sprint.api.AssignSprintRequest;
import com.chila.backend.issue.api.UpdateIssueRequest;
import com.chila.backend.project.ProjectEntity;
import com.chila.backend.project.ProjectNotFoundException;
import com.chila.backend.project.ProjectRepository;
import com.chila.backend.project.ProjectStatus;
import com.chila.backend.user.UserEntity;
import com.chila.backend.user.UserNotFoundException;
import com.chila.backend.user.UserRepository;
import com.chila.backend.notification.NotificationService;
import com.chila.backend.workspace.MembershipEntity;
import com.chila.backend.workspace.MembershipRepository;
import com.chila.backend.workspace.MembershipRole;
import com.chila.backend.workspace.WorkspaceAccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class IssueService {

    private static final long RANK_STEP = 1024;

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final IssueActivityService activityService;
    private final NotificationService notificationService;
    private final SprintRepository sprintRepository;

    public IssueService(
            IssueRepository issueRepository,
            ProjectRepository projectRepository,
            MembershipRepository membershipRepository,
            UserRepository userRepository,
            IssueActivityService activityService,
            NotificationService notificationService,
            SprintRepository sprintRepository) {
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.activityService = activityService;
        this.notificationService = notificationService;
        this.sprintRepository = sprintRepository;
    }

    @Transactional
    public IssueResponse create(UUID actorId, UUID projectId, CreateIssueRequest request) {
        ProjectEntity project = lockProject(projectId);
        requireWorkspaceMember(project, actorId);
        requireActiveProject(project);
        UserEntity reporter = userRepository.findById(actorId).orElseThrow(UserNotFoundException::new);
        UserEntity assignee = resolveAssignee(project, request.assigneeId());
        IssueStatus status = request.status() == null ? IssueStatus.TODO : request.status();
        long issueNumber = project.allocateIssueNumber();
        long rank = nextRank(project.getId(), status);
        long backlogRank = Math.addExact(issueRepository.findMaxBacklogRank(project.getId()), RANK_STEP);
        IssueEntity issue = issueRepository.save(new IssueEntity(project, reporter, request.title().trim(),
                normalizeDescription(request.description()), request.type(), status,
                request.priority() == null ? IssuePriority.MEDIUM : request.priority(),
                assignee, request.dueDate(), issueNumber, rank, backlogRank));
        activityService.record(issue, reporter, "CREATED", null, null, null);
        notificationService.onAssignment(issue, assignee, actorId);
        return response(issue);
    }

    @Transactional(readOnly = true)
    public IssuePageResponse backlog(UUID actorId, UUID projectId, IssuePriority priority, IssueType type,
            UUID assigneeId, boolean unassigned, String search, int page, int size) {
        ProjectEntity project = requireProject(projectId);
        requireWorkspaceMember(project, actorId);
        if (page < 0) throw new InvalidInputException("page", "Page must be zero or greater.");
        if (size < 1 || size > 100) throw new InvalidInputException("size", "Page size must be between 1 and 100.");
        if (unassigned && assigneeId != null) throw new InvalidInputException("assigneeId", "Choose a member or unassigned issues, not both.");
        Page<IssueEntity> results = issueRepository.searchBacklog(projectId,
                StringUtils.hasText(search) ? "%" + search.trim().toLowerCase() + "%" : "%",
                priority, type, assigneeId, unassigned,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "backlogRank").and(Sort.by("id"))));
        return new IssuePageResponse(results.getContent().stream().map(IssueService::response).toList(),
                results.getNumber(), results.getSize(), results.getTotalElements(), results.getTotalPages());
    }

    @Transactional(readOnly = true)
    public IssuePageResponse dueIssues(UUID actorId, UUID projectId, int page, int size) {
        ProjectEntity project = requireProject(projectId);
        requireWorkspaceMember(project, actorId);
        if (page < 0 || size < 1 || size > 100) throw new InvalidInputException("page", "Invalid page or size.");
        Page<IssueEntity> results = issueRepository.findByProject_IdAndDueDateIsNotNullAndArchivedAtIsNull(
                projectId, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "dueDate").and(Sort.by("id"))));
        return new IssuePageResponse(results.getContent().stream().map(IssueService::response).toList(),
                results.getNumber(), results.getSize(), results.getTotalElements(), results.getTotalPages());
    }

    @Transactional
    public IssueResponse assignSprint(UUID actorId, UUID issueId, AssignSprintRequest request) {
        IssueEntity issue = requireIssue(issueId);
        ProjectEntity project = lockProject(issue.getProject().getId());
        requireWorkspaceMember(project, actorId);
        requireActiveProject(project);
        if (issue.getArchivedAt() != null) throw new IssueArchivedException();
        if (issue.getSprint() != null && issue.getSprint().getState() == SprintState.CLOSED) {
            throw new SprintConflictException("Issues in closed sprints cannot be reassigned.");
        }
        SprintEntity target = null;
        if (request.sprintId() != null) {
            target = sprintRepository.findByIdAndProject_Id(request.sprintId(), project.getId())
                    .orElseThrow(SprintNotFoundException::new);
            if (target.getState() == SprintState.CLOSED) throw new SprintConflictException("Issues cannot be added to a closed sprint.");
        }
        if (target == null) {
            long rank;
            if (request.beforeIssueId() == null) {
                long maxRank = issueRepository.findMaxBacklogRankExcluding(project.getId(), issueId);
                rank = issue.getSprint() == null && issue.getBacklogRank() >= maxRank
                        ? issue.getBacklogRank() : Math.addExact(maxRank, RANK_STEP);
            } else {
                IssueEntity before = issueRepository.findBacklogIssue(project.getId(), request.beforeIssueId())
                        .orElseThrow(() -> new InvalidInputException("beforeIssueId", "Choose an issue from this project's backlog."));
                List<IssueEntity> previous = issueRepository.findPreviousBacklogIssue(project.getId(), issueId,
                        before.getBacklogRank(), PageRequest.of(0, 1));
                if (previous.isEmpty() && before.getBacklogRank() > 1) {
                    rank = before.getBacklogRank() / 2;
                } else if (!previous.isEmpty() && before.getBacklogRank() - previous.get(0).getBacklogRank() > 1) {
                    rank = rankBetween(previous.get(0).getBacklogRank(), before.getBacklogRank());
                } else {
                    List<IssueEntity> ordered = issueRepository
                            .findByProject_IdAndSprintIsNullAndArchivedAtIsNullOrderByBacklogRankAscIdAsc(project.getId());
                    ordered.removeIf(existing -> existing.getId().equals(issueId));
                    int index = 0;
                    while (index < ordered.size() && !ordered.get(index).getId().equals(before.getId())) index++;
                    ordered.add(index, issue);
                    for (int position = 0; position < ordered.size(); position++) {
                        ordered.get(position).setBacklogRank((position + 1L) * RANK_STEP);
                    }
                    rank = issue.getBacklogRank();
                }
            }
            issue.assignToSprint(null);
            issue.setBacklogRank(rank);
        } else {
            if (request.beforeIssueId() != null) throw new InvalidInputException("beforeIssueId", "Issue positioning is available only in the backlog.");
            if (issue.getSprint() != null && !issue.getSprint().getId().equals(target.getId())) {
                throw new InvalidInputException("sprintId", "Return the issue to the backlog before planning it in another sprint.");
            }
            issue.assignToSprint(target);
            issue.setBacklogRank(Math.addExact(issueRepository.findMaxBacklogRankInSprint(project.getId(), target.getId()), RANK_STEP));
        }
        return response(issue);
    }

    private static long rankBetween(long left, long right) {
        if (right - left > 1) return left + (right - left) / 2;
        return Math.addExact(left, 1);
    }

    @Transactional(readOnly = true)
    public IssuePageResponse list(
            UUID actorId,
            UUID projectId,
            IssueStatus status,
            IssuePriority priority,
            IssueType type,
            UUID assigneeId,
            boolean unassigned,
            String search,
            boolean includeArchived,
            int page,
            int size,
            String sortField,
            String direction) {
        ProjectEntity project = requireProject(projectId);
        requireWorkspaceMember(project, actorId);
        if (page < 0) throw new InvalidInputException("page", "Page must be zero or greater.");
        if (size < 1 || size > 100) throw new InvalidInputException("size", "Page size must be between 1 and 100.");
        if (unassigned && assigneeId != null) {
            throw new InvalidInputException("assigneeId", "Choose a member or unassigned issues, not both.");
        }
        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new InvalidInputException("direction", "Direction must be asc or desc.");
        }
        String property = switch (sortField) {
            case "createdAt" -> "createdAt";
            case "dueDate" -> "dueDate";
            case "priority" -> "priority";
            case "status" -> "status";
            case "title" -> "title";
            case "rank" -> "rankValue";
            default -> throw new InvalidInputException("sort", "Unsupported issue sort field.");
        };
        Page<IssueEntity> results = issueRepository.search(projectId, status, priority, type, assigneeId,
                unassigned, StringUtils.hasText(search) ? "%" + search.trim().toLowerCase() + "%" : "%", includeArchived,
                PageRequest.of(page, size, Sort.by(sortDirection, property)));
        return new IssuePageResponse(results.getContent().stream().map(IssueService::response).toList(),
                results.getNumber(), results.getSize(), results.getTotalElements(), results.getTotalPages());
    }

    @Transactional(readOnly = true)
    public IssueResponse get(UUID actorId, UUID issueId) {
        IssueEntity issue = requireIssue(issueId);
        requireWorkspaceMember(issue.getProject(), actorId);
        return response(issue);
    }

    @Transactional(readOnly = true)
    public IssueResponse getByKey(UUID actorId, UUID workspaceId, String value) {
        IssueKey key;
        try {
            key = IssueKey.parse(value);
        } catch (IllegalArgumentException exception) {
            throw new IssueNotFoundException();
        }
        IssueEntity issue = issueRepository.findForWorkspaceByKey(workspaceId, key.projectKey(), key.issueNumber())
                .orElseThrow(IssueNotFoundException::new);
        requireWorkspaceMember(issue.getProject(), actorId);
        return response(issue);
    }

    @Transactional
    public IssueResponse update(UUID actorId, UUID issueId, UpdateIssueRequest request) {
        IssueEntity issue = requireIssue(issueId);
        requireWorkspaceMember(issue.getProject(), actorId);
        ProjectEntity project = lockProject(issue.getProject().getId());
        requireActiveProject(project);
        if (issue.getArchivedAt() != null) throw new IssueArchivedException();
        UserEntity assignee = resolveAssignee(issue.getProject(), request.assigneeId());
        if (issue.getSprint() != null && issue.getSprint().getState() == SprintState.CLOSED
                && issue.getStatus() != request.status()) {
            throw new SprintConflictException("Workflow status cannot change for an issue in a closed sprint.");
        }
        validateTransition(issue.getStatus(), request.status());
        UserEntity actor = userRepository.findById(actorId).orElseThrow(UserNotFoundException::new);
        recordChange(issue, actor, "title", issue.getTitle(), request.title().trim());
        recordChange(issue, actor, "description", issue.getDescription(), normalizeDescription(request.description()));
        recordChange(issue, actor, "type", issue.getType().name(), request.type().name());
        recordChange(issue, actor, "status", issue.getStatus().name(), request.status().name());
        recordChange(issue, actor, "priority", issue.getPriority().name(), request.priority().name());
        recordChange(issue, actor, "assignee", issue.getAssignee() == null ? null : issue.getAssignee().getDisplayName(),
                assignee == null ? null : assignee.getDisplayName());
        recordChange(issue, actor, "dueDate", issue.getDueDate() == null ? null : issue.getDueDate().toString(),
                request.dueDate() == null ? null : request.dueDate().toString());
        UUID previousAssigneeId = issue.getAssignee() == null ? null : issue.getAssignee().getId();
        long rank = issue.getStatus() == request.status() ? issue.getRankValue() : nextRank(project.getId(), request.status());
        issue.update(request.title().trim(), normalizeDescription(request.description()), request.type(),
                request.status(), request.priority(), assignee, request.dueDate());
        issue.setRankValue(rank);
        if (assignee != null && !assignee.getId().equals(previousAssigneeId)) {
            notificationService.onAssignment(issue, assignee, actorId);
        }
        return response(issue);
    }

    @Transactional
    public IssueResponse move(UUID actorId, UUID issueId, MoveIssueRequest request) {
        IssueEntity issue = requireIssue(issueId);
        ProjectEntity project = lockProject(issue.getProject().getId());
        requireWorkspaceMember(project, actorId);
        requireActiveProject(project);
        if (issue.getArchivedAt() != null) throw new IssueArchivedException();
        if (issue.getSprint() != null && issue.getSprint().getState() == SprintState.CLOSED) {
            throw new SprintConflictException("Issues in closed sprints cannot be moved on the board.");
        }
        if (issue.getStatus() != request.status()) validateTransition(issue.getStatus(), request.status());

        List<IssueEntity> target = new ArrayList<>(issueRepository
                .findByProject_IdAndStatusAndArchivedAtIsNullOrderByRankValueAscIdAsc(project.getId(), request.status()));
        target.removeIf(item -> item.getId().equals(issue.getId()));
        int insertionIndex = target.size();
        if (request.beforeIssueId() != null) {
            insertionIndex = -1;
            for (int index = 0; index < target.size(); index++) {
                if (target.get(index).getId().equals(request.beforeIssueId())) {
                    insertionIndex = index;
                    break;
                }
            }
            if (insertionIndex < 0) {
                throw new InvalidInputException("beforeIssueId", "The target issue is not in this project's destination column.");
            }
        }
        target.add(insertionIndex, issue);
        long oldRank = issue.getRankValue();
        IssueStatus oldStatus = issue.getStatus();
        long newRank = rankAt(target, insertionIndex);
        issue.moveTo(request.status(), newRank);

        UserEntity actor = userRepository.findById(actorId).orElseThrow(UserNotFoundException::new);
        if (oldStatus != request.status()) {
            activityService.record(issue, actor, "UPDATED", "status", oldStatus.name(), request.status().name());
        }
        if (oldRank != newRank || oldStatus != request.status()) {
            activityService.record(issue, actor, "REORDERED", "rank", Long.toString(oldRank), Long.toString(newRank));
        }
        return response(issue);
    }

    @Transactional
    public void archive(UUID actorId, UUID issueId) {
        IssueEntity issue = requireIssue(issueId);
        requireManager(issue.getProject(), actorId);
        issue.archive();
        UserEntity actor = userRepository.findById(actorId).orElseThrow(UserNotFoundException::new);
        activityService.record(issue, actor, "ARCHIVED", null, null, null);
    }

    @Transactional
    public IssueResponse restore(UUID actorId, UUID issueId) {
        IssueEntity issue = requireIssue(issueId);
        requireManager(issue.getProject(), actorId);
        requireActiveProject(issue.getProject());
        issue.restore();
        UserEntity actor = userRepository.findById(actorId).orElseThrow(UserNotFoundException::new);
        activityService.record(issue, actor, "RESTORED", null, null, null);
        return response(issue);
    }

    private ProjectEntity requireProject(UUID projectId) {
        return projectRepository.findById(projectId).orElseThrow(ProjectNotFoundException::new);
    }

    private ProjectEntity lockProject(UUID projectId) {
        return projectRepository.findByIdForUpdate(projectId).orElseThrow(ProjectNotFoundException::new);
    }

    private long nextRank(UUID projectId, IssueStatus status) {
        long maxRank = issueRepository.findMaxActiveRank(projectId, status);
        if (maxRank <= Long.MAX_VALUE - RANK_STEP) return maxRank + RANK_STEP;
        List<IssueEntity> issues = issueRepository
                .findByProject_IdAndStatusAndArchivedAtIsNullOrderByRankValueAscIdAsc(projectId, status);
        long rank = RANK_STEP;
        for (IssueEntity existing : issues) {
            existing.setRankValue(rank);
            rank = Math.addExact(rank, RANK_STEP);
        }
        return rank;
    }

    private long rankAt(List<IssueEntity> ordered, int index) {
        if (ordered.size() == 1) return RANK_STEP;
        if (index == 0) {
            try {
                return Math.subtractExact(ordered.get(1).getRankValue(), RANK_STEP);
            } catch (ArithmeticException ignored) {
                return rebalanceAndGet(ordered, index);
            }
        }
        if (index == ordered.size() - 1) {
            try {
                return Math.addExact(ordered.get(index - 1).getRankValue(), RANK_STEP);
            } catch (ArithmeticException ignored) {
                return rebalanceAndGet(ordered, index);
            }
        }
        long left = ordered.get(index - 1).getRankValue();
        long right = ordered.get(index + 1).getRankValue();
        BigInteger gap = BigInteger.valueOf(right).subtract(BigInteger.valueOf(left));
        if (gap.compareTo(BigInteger.ONE) > 0) {
            return BigInteger.valueOf(left).add(gap.divide(BigInteger.TWO)).longValueExact();
        }
        return rebalanceAndGet(ordered, index);
    }

    private long rebalanceAndGet(List<IssueEntity> ordered, int targetIndex) {
        long targetRank = 0;
        for (int index = 0; index < ordered.size(); index++) {
            long rank = Math.multiplyExact(index + 1L, RANK_STEP);
            ordered.get(index).setRankValue(rank);
            if (index == targetIndex) targetRank = rank;
        }
        return targetRank;
    }

    private IssueEntity requireIssue(UUID issueId) {
        return issueRepository.findById(issueId).orElseThrow(IssueNotFoundException::new);
    }

    private void requireWorkspaceMember(ProjectEntity project, UUID userId) {
        membershipRepository.findByWorkspaceIdAndUserId(project.getWorkspace().getId(), userId)
                .orElseThrow(ProjectNotFoundException::new);
    }

    private void requireManager(ProjectEntity project, UUID userId) {
        MembershipEntity membership = membershipRepository
                .findByWorkspaceIdAndUserId(project.getWorkspace().getId(), userId)
                .orElseThrow(ProjectNotFoundException::new);
        if (membership.getRole() == MembershipRole.MEMBER) {
            throw new WorkspaceAccessDeniedException();
        }
    }

    private UserEntity resolveAssignee(ProjectEntity project, UUID assigneeId) {
        if (assigneeId == null) return null;
        UUID workspaceId = project.getWorkspace().getId();
        if (!membershipRepository.existsByWorkspaceIdAndUserId(workspaceId, assigneeId)) {
            throw new InvalidInputException("assigneeId", "Assignee must be a member of this workspace.");
        }
        return userRepository.findById(assigneeId).orElseThrow(UserNotFoundException::new);
    }

    private static void requireActiveProject(ProjectEntity project) {
        if (project.getStatus() != ProjectStatus.ACTIVE) throw new ArchivedProjectException();
    }

    private void recordChange(IssueEntity issue, UserEntity actor, String field, String oldValue, String newValue) {
        if (!java.util.Objects.equals(oldValue, newValue)) {
            activityService.record(issue, actor, "UPDATED", field, oldValue, newValue);
        }
    }

    private static void validateTransition(IssueStatus from, IssueStatus to) {
        if (from == to) return;
        boolean allowed = switch (from) {
            case TODO -> to == IssueStatus.IN_PROGRESS;
            case IN_PROGRESS -> to == IssueStatus.TODO || to == IssueStatus.IN_REVIEW;
            case IN_REVIEW -> to == IssueStatus.IN_PROGRESS || to == IssueStatus.DONE;
            case DONE -> to == IssueStatus.IN_PROGRESS;
        };
        if (!allowed) throw new InvalidStatusTransitionException();
    }

    private static String normalizeDescription(String description) {
        return StringUtils.hasText(description) ? description.trim() : null;
    }

    public static IssueResponse response(IssueEntity issue) {
        UserEntity reporter = issue.getReporter();
        UserEntity assignee = issue.getAssignee();
        String projectKey = issue.getProject().getKey();
        return new IssueResponse(issue.getId(), issue.getProject().getId(),
                issue.getSprint() == null ? null : issue.getSprint().getId(),
                issue.getEpic() == null ? null : issue.getEpic().getId(),
                issue.getParentIssue() == null ? null : issue.getParentIssue().getId(),
                issue.getRelease() == null ? null : issue.getRelease().getId(), projectKey,
                projectKey + "-" + issue.getIssueNumber(), issue.getIssueNumber(),
                issue.getTitle(), issue.getDescription(), issue.getType(), issue.getStatus(), issue.getPriority(),
                reporter.getId(), reporter.getDisplayName(), assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getDisplayName(), issue.getDueDate(), issue.getArchivedAt(),
                issue.getCreatedAt(), issue.getUpdatedAt());
    }
}
