package com.chila.backend.comment;

import java.net.URI;
import java.util.UUID;

import com.chila.backend.activity.IssueActivityController;
import com.chila.backend.comment.api.CreateIssueCommentRequest;
import com.chila.backend.comment.api.IssueCommentPageResponse;
import com.chila.backend.comment.api.IssueCommentResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IssueCommentController {
    private final IssueCommentService service;
    public IssueCommentController(IssueCommentService service) { this.service = service; }
    @GetMapping("/api/v1/issues/{issueId}/comments")
    public IssueCommentPageResponse list(JwtAuthenticationToken auth, @PathVariable UUID issueId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        IssueActivityController.validate(page, size);
        return service.list(userId(auth), issueId, page, size);
    }
    @PostMapping("/api/v1/issues/{issueId}/comments")
    public ResponseEntity<IssueCommentResponse> create(JwtAuthenticationToken auth, @PathVariable UUID issueId,
            @Valid @RequestBody CreateIssueCommentRequest request) {
        IssueCommentResponse response = service.create(userId(auth), issueId, request);
        return ResponseEntity.created(URI.create("/api/v1/issues/" + issueId + "/comments/" + response.id())).body(response);
    }
    private static UUID userId(JwtAuthenticationToken auth) { return UUID.fromString(auth.getToken().getSubject()); }
}
