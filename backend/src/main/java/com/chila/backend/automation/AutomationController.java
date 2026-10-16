package com.chila.backend.automation;

import java.util.List;
import java.util.UUID;
import com.chila.backend.automation.api.*;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AutomationController {
    private final AutomationService service;
    public AutomationController(AutomationService service) { this.service = service; }

    @GetMapping("/api/v1/projects/{projectId}/automation/rules")
    public List<AutomationRuleResponse> list(JwtAuthenticationToken auth, @PathVariable UUID projectId) {
        return service.listRules(userId(auth), projectId);
    }
    @PostMapping("/api/v1/projects/{projectId}/automation/rules")
    public ResponseEntity<AutomationRuleResponse> create(JwtAuthenticationToken auth, @PathVariable UUID projectId,
            @Valid @RequestBody AutomationRuleRequest request) {
        AutomationRuleResponse rule = service.create(userId(auth), projectId, request);
        return ResponseEntity.created(URI.create("/api/v1/projects/" + projectId + "/automation/rules/" + rule.id())).body(rule);
    }
    @PutMapping("/api/v1/projects/{projectId}/automation/rules/{ruleId}")
    public AutomationRuleResponse update(JwtAuthenticationToken auth, @PathVariable UUID projectId,
            @PathVariable UUID ruleId, @Valid @RequestBody AutomationRuleRequest request) {
        return service.update(userId(auth), projectId, ruleId, request);
    }
    @DeleteMapping("/api/v1/projects/{projectId}/automation/rules/{ruleId}")
    public void delete(JwtAuthenticationToken auth, @PathVariable UUID projectId, @PathVariable UUID ruleId) {
        service.delete(userId(auth), projectId, ruleId);
    }
    @GetMapping("/api/v1/projects/{projectId}/automation/executions")
    public List<AutomationExecutionResponse> executions(JwtAuthenticationToken auth, @PathVariable UUID projectId) {
        return service.listExecutions(userId(auth), projectId);
    }
    private static UUID userId(JwtAuthenticationToken auth) { return UUID.fromString(auth.getToken().getSubject()); }
}
