package com.chila.backend.project;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.chila.backend.common.api.InvalidInputException;
import com.chila.backend.user.UserEntity;
import com.chila.backend.user.UserNotFoundException;
import com.chila.backend.user.UserRepository;
import com.chila.backend.workspace.MembershipEntity;
import com.chila.backend.workspace.MembershipRepository;
import com.chila.backend.workspace.MembershipRole;
import com.chila.backend.workspace.WorkspaceAccessDeniedException;
import com.chila.backend.workspace.WorkspaceNotFoundException;
import com.chila.backend.project.api.CreateProjectRequest;
import com.chila.backend.project.api.ProjectResponse;
import com.chila.backend.project.api.UpdateProjectRequest;
import com.chila.backend.project.api.UpdateProjectStatusRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            MembershipRepository membershipRepository,
            UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProjectResponse create(UUID actorId, UUID workspaceId, CreateProjectRequest request) {
        MembershipEntity membership = requireMembership(workspaceId, actorId);
        UserEntity creator = userRepository.findById(actorId).orElseThrow(UserNotFoundException::new);
        String key = request.key().trim().toUpperCase(Locale.ROOT);
        if (projectRepository.existsByWorkspace_IdAndKey(workspaceId, key)) {
            throw new DuplicateProjectKeyException();
        }
        ProjectEntity project = projectRepository.save(new ProjectEntity(
                membership.getWorkspace(), creator, request.name().trim(), key, normalizeDescription(request.description())));
        return response(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(UUID actorId, UUID workspaceId) {
        requireMembership(workspaceId, actorId);
        return projectRepository.findAllForWorkspace(workspaceId).stream()
                .map(ProjectService::response)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID actorId, UUID projectId) {
        ProjectEntity project = requireProject(projectId);
        requireMembership(project.getWorkspace().getId(), actorId);
        return response(project);
    }

    @Transactional
    public ProjectResponse update(UUID actorId, UUID projectId, UpdateProjectRequest request) {
        ProjectEntity project = requireProject(projectId);
        requireManager(project.getWorkspace().getId(), actorId);
        project.updateDetails(request.name().trim(), normalizeDescription(request.description()));
        return response(project);
    }

    @Transactional
    public ProjectResponse updateStatus(UUID actorId, UUID projectId, UpdateProjectStatusRequest request) {
        ProjectEntity project = projectRepository.findByIdForUpdate(projectId).orElseThrow(ProjectNotFoundException::new);
        requireManager(project.getWorkspace().getId(), actorId);
        project.setStatus(request.status());
        return response(project);
    }

    private ProjectEntity requireProject(UUID projectId) {
        return projectRepository.findById(projectId).orElseThrow(ProjectNotFoundException::new);
    }

    private MembershipEntity requireMembership(UUID workspaceId, UUID userId) {
        return membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(WorkspaceNotFoundException::new);
    }

    private void requireManager(UUID workspaceId, UUID userId) {
        MembershipRole role = requireMembership(workspaceId, userId).getRole();
        if (role == MembershipRole.MEMBER) {
            throw new WorkspaceAccessDeniedException();
        }
    }

    private static String normalizeDescription(String description) {
        return StringUtils.hasText(description) ? description.trim() : null;
    }

    private static ProjectResponse response(ProjectEntity project) {
        return new ProjectResponse(project.getId(), project.getWorkspace().getId(), project.getCreatedBy().getId(),
                project.getName(), project.getKey(), project.getDescription(), project.getStatus(),
                project.getCreatedAt(), project.getUpdatedAt());
    }
}
