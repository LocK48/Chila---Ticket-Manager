package com.chila.backend.project;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.chila.backend.project.api.CreateProjectRequest;
import com.chila.backend.project.api.ProjectResponse;
import com.chila.backend.project.api.UpdateProjectRequest;
import com.chila.backend.project.api.UpdateProjectStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/projects")
    public ResponseEntity<ProjectResponse> create(
            JwtAuthenticationToken authentication,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse project = projectService.create(userId(authentication), workspaceId, request);
        return ResponseEntity.created(URI.create("/api/v1/projects/" + project.id())).body(project);
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/projects")
    public List<ProjectResponse> list(
            JwtAuthenticationToken authentication, @PathVariable UUID workspaceId) {
        return projectService.list(userId(authentication), workspaceId);
    }

    @GetMapping("/api/v1/projects/{projectId}")
    public ProjectResponse get(JwtAuthenticationToken authentication, @PathVariable UUID projectId) {
        return projectService.get(userId(authentication), projectId);
    }

    @PatchMapping("/api/v1/projects/{projectId}")
    public ProjectResponse update(
            JwtAuthenticationToken authentication,
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.update(userId(authentication), projectId, request);
    }

    @PatchMapping("/api/v1/projects/{projectId}/status")
    public ProjectResponse updateStatus(
            JwtAuthenticationToken authentication,
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectStatusRequest request) {
        return projectService.updateStatus(userId(authentication), projectId, request);
    }

    private static UUID userId(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getToken().getSubject());
    }
}
