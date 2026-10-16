package com.chila.backend.hierarchy;

import java.util.List;
import java.util.UUID;
import com.chila.backend.hierarchy.api.*;
import com.chila.backend.issue.IssueStatus;
import com.chila.backend.issue.api.IssuePageResponse;
import com.chila.backend.issue.api.IssueResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class HierarchyController {
    private final HierarchyService service;
    public HierarchyController(HierarchyService service) { this.service = service; }

    @GetMapping("/api/v1/projects/{projectId}/epics")
    public List<EpicResponse> epics(JwtAuthenticationToken auth, @PathVariable UUID projectId) {
        return service.listEpics(userId(auth), projectId);
    }
    @PostMapping("/api/v1/projects/{projectId}/epics")
    @ResponseStatus(HttpStatus.CREATED)
    public EpicResponse createEpic(JwtAuthenticationToken auth, @PathVariable UUID projectId, @Valid @RequestBody EpicRequest request) {
        return service.createEpic(userId(auth), projectId, request);
    }
    @PutMapping("/api/v1/projects/{projectId}/epics/{epicId}")
    public EpicResponse updateEpic(JwtAuthenticationToken auth, @PathVariable UUID projectId, @PathVariable UUID epicId,
            @Valid @RequestBody EpicRequest request) {
        return service.updateEpic(userId(auth), projectId, epicId, request);
    }
    @GetMapping("/api/v1/projects/{projectId}/epics/{epicId}/issues")
    public IssuePageResponse epicIssues(JwtAuthenticationToken auth, @PathVariable UUID projectId, @PathVariable UUID epicId,
            @RequestParam(required = false) IssueStatus status, @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return service.epicIssues(userId(auth), projectId, epicId, status, search, page, size);
    }

    @GetMapping("/api/v1/projects/{projectId}/releases")
    public List<ReleaseResponse> releases(JwtAuthenticationToken auth, @PathVariable UUID projectId) {
        return service.listReleases(userId(auth), projectId);
    }
    @PostMapping("/api/v1/projects/{projectId}/releases")
    @ResponseStatus(HttpStatus.CREATED)
    public ReleaseResponse createRelease(JwtAuthenticationToken auth, @PathVariable UUID projectId, @Valid @RequestBody ReleaseRequest request) {
        return service.createRelease(userId(auth), projectId, request);
    }
    @PutMapping("/api/v1/projects/{projectId}/releases/{releaseId}")
    public ReleaseResponse updateRelease(JwtAuthenticationToken auth, @PathVariable UUID projectId, @PathVariable UUID releaseId,
            @Valid @RequestBody ReleaseRequest request) {
        return service.updateRelease(userId(auth), projectId, releaseId, request);
    }

    @PutMapping("/api/v1/issues/{issueId}/epic")
    public IssueResponse assignEpic(JwtAuthenticationToken auth, @PathVariable UUID issueId, @RequestBody IdAssignmentRequest request) {
        return service.assignEpic(userId(auth), issueId, request);
    }
    @PutMapping("/api/v1/issues/{issueId}/parent")
    public IssueResponse setParent(JwtAuthenticationToken auth, @PathVariable UUID issueId, @RequestBody IdAssignmentRequest request) {
        return service.setParent(userId(auth), issueId, request);
    }
    @PutMapping("/api/v1/issues/{issueId}/release")
    public IssueResponse assignRelease(JwtAuthenticationToken auth, @PathVariable UUID issueId, @RequestBody IdAssignmentRequest request) {
        return service.assignRelease(userId(auth), issueId, request);
    }
    @GetMapping("/api/v1/issues/{issueId}/relationships")
    public IssueRelationshipsResponse relationships(JwtAuthenticationToken auth, @PathVariable UUID issueId) {
        return service.relationships(userId(auth), issueId);
    }
    @PostMapping("/api/v1/issues/{issueId}/links")
    @ResponseStatus(HttpStatus.CREATED)
    public IssueLinkResponse addLink(JwtAuthenticationToken auth, @PathVariable UUID issueId, @Valid @RequestBody IssueLinkRequest request) {
        return service.addLink(userId(auth), issueId, request);
    }
    @DeleteMapping("/api/v1/issues/{issueId}/links/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeLink(JwtAuthenticationToken auth, @PathVariable UUID issueId, @PathVariable UUID linkId) {
        service.removeLink(userId(auth), issueId, linkId);
    }
    private static UUID userId(JwtAuthenticationToken authentication) { return UUID.fromString(authentication.getToken().getSubject()); }
}
