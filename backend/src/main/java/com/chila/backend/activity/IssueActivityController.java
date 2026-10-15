package com.chila.backend.activity;

import java.util.UUID;

import com.chila.backend.activity.api.IssueActivityPageResponse;
import com.chila.backend.common.api.InvalidInputException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@RestController
public class IssueActivityController {
    private final IssueActivityService service;
    public IssueActivityController(IssueActivityService service) { this.service = service; }
    @GetMapping("/api/v1/issues/{issueId}/activities")
    public IssueActivityPageResponse list(JwtAuthenticationToken auth, @PathVariable UUID issueId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        validate(page, size);
        return service.list(userId(auth), issueId, page, size);
    }
    public static void validate(int page, int size) {
        if (page < 0) throw new InvalidInputException("page", "Page must be zero or greater.");
        if (size < 1 || size > 100) throw new InvalidInputException("size", "Page size must be between 1 and 100.");
    }
    private static UUID userId(JwtAuthenticationToken auth) { return UUID.fromString(auth.getToken().getSubject()); }
}
