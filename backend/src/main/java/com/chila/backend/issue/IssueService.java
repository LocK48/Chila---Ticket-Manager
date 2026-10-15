package com.chila.backend.issue;

import java.util.UUID;

import com.chila.backend.common.api.InvalidInputException;
import com.chila.backend.issue.api.CreateIssueRequest;
import com.chila.backend.issue.api.IssuePageResponse;
import com.chila.backend.issue.api.IssueResponse;
import com.chila.backend.issue.api.UpdateIssueRequest;
import com.chila.backend.project.ProjectEntity;
import com.chila.backend.project.ProjectNotFoundException;
import com.chila.backend.project.ProjectRepository;
import com.chila.backend.project.ProjectStatus;
import com.chila.backend.user.UserEntity;
import com.chila.backend.user.UserNotFoundException;
import com.chila.backend.user.UserRepository;
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

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public IssueService(
            IssueRepository issueRepository,
            ProjectRepository projectRepository,
            MembershipRepository membershipRepository,
            UserRepository userRepository) {
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public IssueResponse create(UUID actorId, UUID projectId, CreateIssueRequest request) {
        ProjectEntity project = requireProject(projectId);
        requireWorkspaceMember(project, actorId);
        requireActiveProject(project);
        UserEntity reporter = userRepository.findById(actorId).orElseThrow(UserNotFoundException::new);
        UserEntity assignee = resolveAssignee(project, request.assigneeId());
        IssueEntity issue = issueRepository.save(new IssueEntity(project, reporter, request.title().trim(),
                normalizeDescription(request.description()), request.type(),
                request.status() == null ? IssueStatus.TODO : request.status(),
                request.priority() == null ? IssuePriority.MEDIUM : request.priority(),
                assignee, request.dueDate()));
        return response(issue);
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

    @Transactional
    public IssueResponse update(UUID actorId, UUID issueId, UpdateIssueRequest request) {
        IssueEntity issue = requireIssue(issueId);
        requireWorkspaceMember(issue.getProject(), actorId);
        requireActiveProject(issue.getProject());
        if (issue.getArchivedAt() != null) throw new IssueArchivedException();
        UserEntity assignee = resolveAssignee(issue.getProject(), request.assigneeId());
        issue.update(request.title().trim(), normalizeDescription(request.description()), request.type(),
                request.status(), request.priority(), assignee, request.dueDate());
        return response(issue);
    }

    @Transactional
    public void archive(UUID actorId, UUID issueId) {
        IssueEntity issue = requireIssue(issueId);
        requireManager(issue.getProject(), actorId);
        issue.archive();
    }

    @Transactional
    public IssueResponse restore(UUID actorId, UUID issueId) {
        IssueEntity issue = requireIssue(issueId);
        requireManager(issue.getProject(), actorId);
        requireActiveProject(issue.getProject());
        issue.restore();
        return response(issue);
    }

    private ProjectEntity requireProject(UUID projectId) {
        return projectRepository.findById(projectId).orElseThrow(ProjectNotFoundException::new);
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

    private static String normalizeDescription(String description) {
        return StringUtils.hasText(description) ? description.trim() : null;
    }

    private static IssueResponse response(IssueEntity issue) {
        UserEntity reporter = issue.getReporter();
        UserEntity assignee = issue.getAssignee();
        return new IssueResponse(issue.getId(), issue.getProject().getId(), issue.getProject().getKey(),
                issue.getTitle(), issue.getDescription(), issue.getType(), issue.getStatus(), issue.getPriority(),
                reporter.getId(), reporter.getDisplayName(), assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getDisplayName(), issue.getDueDate(), issue.getArchivedAt(),
                issue.getCreatedAt(), issue.getUpdatedAt());
    }
}
