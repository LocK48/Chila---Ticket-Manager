package com.chila.backend.sprint.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.chila.backend.sprint.SprintService;
import com.chila.backend.issue.IssueStatus;
import com.chila.backend.issue.IssuePriority;
import com.chila.backend.issue.IssueType;
import com.chila.backend.issue.api.IssuePageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SprintController {
    private final SprintService sprintService;
    public SprintController(SprintService sprintService) { this.sprintService = sprintService; }

    @GetMapping("/api/v1/projects/{projectId}/sprints")
    public List<SprintResponse> list(JwtAuthenticationToken auth, @PathVariable UUID projectId) {
        return sprintService.list(userId(auth), projectId);
    }

    @PostMapping("/api/v1/projects/{projectId}/sprints")
    public ResponseEntity<SprintResponse> create(JwtAuthenticationToken auth, @PathVariable UUID projectId,
            @Valid @RequestBody SprintRequest request) {
        SprintResponse sprint = sprintService.create(userId(auth), projectId, request);
        return ResponseEntity.created(URI.create("/api/v1/projects/" + projectId + "/sprints/" + sprint.id())).body(sprint);
    }

    @PatchMapping("/api/v1/projects/{projectId}/sprints/{sprintId}")
    public SprintResponse update(JwtAuthenticationToken auth, @PathVariable UUID projectId,
            @PathVariable UUID sprintId, @Valid @RequestBody SprintRequest request) {
        return sprintService.update(userId(auth), projectId, sprintId, request);
    }

    @PostMapping("/api/v1/projects/{projectId}/sprints/{sprintId}/start")
    public SprintResponse start(JwtAuthenticationToken auth, @PathVariable UUID projectId, @PathVariable UUID sprintId) {
        return sprintService.start(userId(auth), projectId, sprintId);
    }

    @PostMapping("/api/v1/projects/{projectId}/sprints/{sprintId}/complete")
    public SprintResponse complete(JwtAuthenticationToken auth, @PathVariable UUID projectId,
            @PathVariable UUID sprintId, @Valid @RequestBody CompleteSprintRequest request) {
        return sprintService.complete(userId(auth), projectId, sprintId, request);
    }

    @PostMapping("/api/v1/projects/{projectId}/sprints/{sprintId}/issues")
    public ResponseEntity<Void> addIssues(JwtAuthenticationToken auth, @PathVariable UUID projectId,
            @PathVariable UUID sprintId, @Valid @RequestBody SprintIssueSelectionRequest request) {
        sprintService.addIssues(userId(auth), projectId, sprintId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/projects/{projectId}/sprints/{sprintId}/remove-issues")
    public ResponseEntity<Void> removeIssues(JwtAuthenticationToken auth, @PathVariable UUID projectId,
            @PathVariable UUID sprintId, @Valid @RequestBody SprintIssueSelectionRequest request) {
        sprintService.removeIssues(userId(auth), projectId, sprintId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/projects/{projectId}/sprints/{sprintId}/issues")
    public IssuePageResponse issues(JwtAuthenticationToken auth, @PathVariable UUID projectId,
            @PathVariable UUID sprintId, @org.springframework.web.bind.annotation.RequestParam(required = false) IssueStatus status,
            @org.springframework.web.bind.annotation.RequestParam(required = false) IssuePriority priority,
            @org.springframework.web.bind.annotation.RequestParam(required = false) IssueType type,
            @org.springframework.web.bind.annotation.RequestParam(required = false) UUID assigneeId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean unassigned,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "50") int size) {
        return sprintService.issues(userId(auth), projectId, sprintId, status, priority, type,
                assigneeId, unassigned, search, page, size);
    }

    private static UUID userId(JwtAuthenticationToken auth) {
        return UUID.fromString(auth.getToken().getSubject());
    }
}
