package com.chila.backend.workspace;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.chila.backend.workspace.api.AddMemberRequest;
import com.chila.backend.workspace.api.ChangeMemberRoleRequest;
import com.chila.backend.workspace.api.CreateWorkspaceRequest;
import com.chila.backend.workspace.api.MemberResponse;
import com.chila.backend.workspace.api.UpdateWorkspaceRequest;
import com.chila.backend.workspace.api.WorkspaceResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> create(
            JwtAuthenticationToken authentication, @Valid @RequestBody CreateWorkspaceRequest request) {
        WorkspaceResponse created = workspaceService.create(userId(authentication), request);
        return ResponseEntity.created(URI.create("/api/v1/workspaces/" + created.id())).body(created);
    }

    @GetMapping
    public List<WorkspaceResponse> list(JwtAuthenticationToken authentication) {
        return workspaceService.listForUser(userId(authentication));
    }

    @GetMapping("/{workspaceId}")
    public WorkspaceResponse get(JwtAuthenticationToken authentication, @PathVariable UUID workspaceId) {
        return workspaceService.get(userId(authentication), workspaceId);
    }

    @PatchMapping("/{workspaceId}")
    public WorkspaceResponse update(
            JwtAuthenticationToken authentication,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody UpdateWorkspaceRequest request) {
        return workspaceService.update(userId(authentication), workspaceId, request);
    }

    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<Void> delete(JwtAuthenticationToken authentication, @PathVariable UUID workspaceId) {
        workspaceService.delete(userId(authentication), workspaceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{workspaceId}/members")
    public List<MemberResponse> listMembers(
            JwtAuthenticationToken authentication, @PathVariable UUID workspaceId) {
        return workspaceService.listMembers(userId(authentication), workspaceId);
    }

    @PostMapping("/{workspaceId}/members")
    public ResponseEntity<MemberResponse> addMember(
            JwtAuthenticationToken authentication,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody AddMemberRequest request) {
        MemberResponse member = workspaceService.addMember(userId(authentication), workspaceId, request);
        return ResponseEntity.created(URI.create("/api/v1/workspaces/" + workspaceId + "/members/" + member.userId()))
                .body(member);
    }

    @PatchMapping("/{workspaceId}/members/{userId}")
    public MemberResponse changeRole(
            JwtAuthenticationToken authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeMemberRoleRequest request) {
        return workspaceService.changeRole(userId(authentication), workspaceId, userId, request);
    }

    @DeleteMapping("/{workspaceId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            JwtAuthenticationToken authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID userId) {
        workspaceService.removeMember(userId(authentication), workspaceId, userId);
        return ResponseEntity.noContent().build();
    }

    private static UUID userId(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getToken().getSubject());
    }
}
