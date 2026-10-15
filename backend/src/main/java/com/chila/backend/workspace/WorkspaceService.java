package com.chila.backend.workspace;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.chila.backend.common.api.InvalidInputException;
import com.chila.backend.user.UserEntity;
import com.chila.backend.user.UserNotFoundException;
import com.chila.backend.user.UserRepository;
import com.chila.backend.workspace.api.AddMemberRequest;
import com.chila.backend.workspace.api.ChangeMemberRoleRequest;
import com.chila.backend.workspace.api.CreateWorkspaceRequest;
import com.chila.backend.workspace.api.MemberResponse;
import com.chila.backend.workspace.api.MembershipRoleRequest;
import com.chila.backend.workspace.api.UpdateWorkspaceRequest;
import com.chila.backend.workspace.api.WorkspaceResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            MembershipRepository membershipRepository,
            UserRepository userRepository) {
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public WorkspaceResponse create(UUID userId, CreateWorkspaceRequest request) {
        UserEntity creator = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        WorkspaceEntity workspace = workspaceRepository.save(new WorkspaceEntity(
                request.name().trim(), normalizeDescription(request.description()), creator));
        membershipRepository.save(new MembershipEntity(workspace, creator, MembershipRole.OWNER));
        return response(workspace, MembershipRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listForUser(UUID userId) {
        return membershipRepository.findAllForUser(userId).stream()
                .map(membership -> response(membership.getWorkspace(), membership.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse get(UUID userId, UUID workspaceId) {
        MembershipEntity membership = requireMembership(workspaceId, userId);
        return response(membership.getWorkspace(), membership.getRole());
    }

    @Transactional
    public WorkspaceResponse update(UUID userId, UUID workspaceId, UpdateWorkspaceRequest request) {
        MembershipEntity membership = requireManager(workspaceId, userId);
        WorkspaceEntity workspace = membership.getWorkspace();
        workspace.update(request.name().trim(), normalizeDescription(request.description()));
        return response(workspace, membership.getRole());
    }

    @Transactional
    public void delete(UUID userId, UUID workspaceId) {
        MembershipEntity membership = requireMembership(workspaceId, userId);
        if (membership.getRole() != MembershipRole.OWNER) {
            throw new WorkspaceAccessDeniedException();
        }
        membershipRepository.deleteAllByWorkspaceId(workspaceId);
        workspaceRepository.delete(membership.getWorkspace());
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(UUID userId, UUID workspaceId) {
        requireMembership(workspaceId, userId);
        return membershipRepository.findByWorkspaceIdOrderByJoinedAtAsc(workspaceId).stream()
                .map(WorkspaceService::memberResponse)
                .toList();
    }

    @Transactional
    public MemberResponse addMember(UUID actorId, UUID workspaceId, AddMemberRequest request) {
        MembershipEntity actor = requireManager(workspaceId, actorId);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        UserEntity user = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);
        if (membershipRepository.existsByWorkspaceIdAndUserId(workspaceId, user.getId())) {
            throw new MembershipConflictException();
        }

        MembershipRole role = request.role() == null ? MembershipRole.MEMBER
                : MembershipRole.valueOf(request.role().name());
        return memberResponse(membershipRepository.save(
                new MembershipEntity(actor.getWorkspace(), user, role)));
    }

    @Transactional
    public MemberResponse changeRole(
            UUID actorId, UUID workspaceId, UUID targetUserId, ChangeMemberRoleRequest request) {
        MembershipEntity actor = requireManager(workspaceId, actorId);
        MembershipEntity target = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(WorkspaceNotFoundException::new);
        if (target.getRole() == MembershipRole.OWNER || request.role() == MembershipRole.OWNER) {
            throw new InvalidInputException("role", "Workspace ownership cannot be changed through this endpoint.");
        }
        if (actor.getRole() == MembershipRole.ADMIN && target.getRole() == MembershipRole.ADMIN) {
            throw new WorkspaceAccessDeniedException();
        }
        target.setRole(request.role());
        return memberResponse(target);
    }

    @Transactional
    public void removeMember(UUID actorId, UUID workspaceId, UUID targetUserId) {
        MembershipEntity actor = requireMembership(workspaceId, actorId);
        MembershipEntity target = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(WorkspaceNotFoundException::new);
        if (target.getRole() == MembershipRole.OWNER) {
            throw new InvalidInputException("userId", "The workspace owner cannot be removed.");
        }
        boolean selfRemoval = actorId.equals(targetUserId);
        if (!selfRemoval) {
            if (actor.getRole() == MembershipRole.MEMBER
                    || (actor.getRole() == MembershipRole.ADMIN && target.getRole() == MembershipRole.ADMIN)) {
                throw new WorkspaceAccessDeniedException();
            }
        }
        membershipRepository.delete(target);
    }

    private MembershipEntity requireManager(UUID workspaceId, UUID userId) {
        MembershipEntity membership = requireMembership(workspaceId, userId);
        if (membership.getRole() == MembershipRole.MEMBER) {
            throw new WorkspaceAccessDeniedException();
        }
        return membership;
    }

    private MembershipEntity requireMembership(UUID workspaceId, UUID userId) {
        return membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(WorkspaceNotFoundException::new);
    }

    private static String normalizeDescription(String description) {
        return StringUtils.hasText(description) ? description.trim() : null;
    }

    private static WorkspaceResponse response(WorkspaceEntity workspace, MembershipRole role) {
        return new WorkspaceResponse(workspace.getId(), workspace.getName(), workspace.getDescription(), role,
                workspace.getCreatedAt(), workspace.getUpdatedAt());
    }

    private static MemberResponse memberResponse(MembershipEntity membership) {
        UserEntity user = membership.getUser();
        return new MemberResponse(user.getId(), user.getEmail(), user.getDisplayName(), membership.getRole(),
                membership.getJoinedAt());
    }
}
