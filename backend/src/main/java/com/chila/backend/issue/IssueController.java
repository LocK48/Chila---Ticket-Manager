package com.chila.backend.issue;

import java.net.URI;
import java.util.UUID;
import java.time.LocalDate;

import com.chila.backend.issue.api.CreateIssueRequest;
import com.chila.backend.issue.api.IssuePageResponse;
import com.chila.backend.issue.api.IssueResponse;
import com.chila.backend.issue.api.MoveIssueRequest;
import com.chila.backend.issue.api.UpdateIssueRequest;
import com.chila.backend.sprint.api.AssignSprintRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping("/api/v1/projects/{projectId}/issues")
    public ResponseEntity<IssueResponse> create(
            JwtAuthenticationToken authentication,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateIssueRequest request) {
        IssueResponse issue = issueService.create(userId(authentication), projectId, request);
        return ResponseEntity.created(URI.create("/api/v1/issues/" + issue.id())).body(issue);
    }

    @GetMapping("/api/v1/projects/{projectId}/issues")
    public IssuePageResponse list(
            JwtAuthenticationToken authentication,
            @PathVariable UUID projectId,
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) IssuePriority priority,
            @RequestParam(required = false) IssueType type,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(defaultValue = "false") boolean unassigned,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(required = false) UUID sprintId,
            @RequestParam(required = false) UUID epicId,
            @RequestParam(required = false) LocalDate dueFrom,
            @RequestParam(required = false) LocalDate dueTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return issueService.list(userId(authentication), projectId, status, priority, type, assigneeId,
                unassigned, search, includeArchived, sprintId, epicId, dueFrom, dueTo, page, size, sort, direction);
    }

    @GetMapping("/api/v1/projects/{projectId}/backlog")
    public IssuePageResponse backlog(JwtAuthenticationToken authentication, @PathVariable UUID projectId,
            @RequestParam(required = false) IssuePriority priority, @RequestParam(required = false) IssueType type,
            @RequestParam(required = false) UUID assigneeId, @RequestParam(defaultValue = "false") boolean unassigned,
            @RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return issueService.backlog(userId(authentication), projectId, priority, type, assigneeId,
                unassigned, search, page, size);
    }

    @GetMapping("/api/v1/projects/{projectId}/timeline/due-issues")
    public IssuePageResponse dueIssues(JwtAuthenticationToken authentication, @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) {
        return issueService.dueIssues(userId(authentication), projectId, page, size);
    }

    @GetMapping("/api/v1/issues/{issueId}")
    public IssueResponse get(JwtAuthenticationToken authentication, @PathVariable UUID issueId) {
        return issueService.get(userId(authentication), issueId);
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/issues/{issueKey}")
    public IssueResponse getByKey(JwtAuthenticationToken authentication, @PathVariable UUID workspaceId,
            @PathVariable String issueKey) {
        return issueService.getByKey(userId(authentication), workspaceId, issueKey);
    }

    @PatchMapping("/api/v1/issues/{issueId}")
    public IssueResponse update(
            JwtAuthenticationToken authentication,
            @PathVariable UUID issueId,
            @Valid @RequestBody UpdateIssueRequest request) {
        return issueService.update(userId(authentication), issueId, request);
    }

    @PatchMapping("/api/v1/issues/{issueId}/position")
    public IssueResponse move(JwtAuthenticationToken authentication, @PathVariable UUID issueId,
            @Valid @RequestBody MoveIssueRequest request) {
        return issueService.move(userId(authentication), issueId, request);
    }

    @PatchMapping("/api/v1/issues/{issueId}/sprint")
    public IssueResponse assignSprint(JwtAuthenticationToken authentication, @PathVariable UUID issueId,
            @Valid @RequestBody AssignSprintRequest request) {
        return issueService.assignSprint(userId(authentication), issueId, request);
    }

    @DeleteMapping("/api/v1/issues/{issueId}")
    public ResponseEntity<Void> archive(JwtAuthenticationToken authentication, @PathVariable UUID issueId) {
        issueService.archive(userId(authentication), issueId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/issues/{issueId}/restore")
    public IssueResponse restore(JwtAuthenticationToken authentication, @PathVariable UUID issueId) {
        return issueService.restore(userId(authentication), issueId);
    }

    private static UUID userId(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getToken().getSubject());
    }
}
