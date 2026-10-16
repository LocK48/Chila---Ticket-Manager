package com.chila.backend.workflow;

import java.util.List;
import java.util.UUID;
import com.chila.backend.workflow.api.*;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class WorkflowController {
    private final WorkflowService service;
    public WorkflowController(WorkflowService service) { this.service = service; }
    @GetMapping("/api/v1/projects/{projectId}/workflow")
    public WorkflowResponse get(JwtAuthenticationToken auth, @PathVariable UUID projectId) { return service.get(userId(auth), projectId); }
    @PutMapping("/api/v1/projects/{projectId}/workflow/transitions")
    public WorkflowResponse update(JwtAuthenticationToken auth, @PathVariable UUID projectId,
            @RequestBody List<@Valid WorkflowTransitionInput> transitions) { return service.update(userId(auth), projectId, transitions); }
    private static UUID userId(JwtAuthenticationToken auth) { return UUID.fromString(auth.getToken().getSubject()); }
}
