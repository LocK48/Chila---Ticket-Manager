package com.chila.backend.report;

import java.util.UUID;
import com.chila.backend.report.api.ProjectDashboardResponse;
import com.chila.backend.report.api.SprintReportResponse;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReportController {
    private final ReportService reports;
    public ReportController(ReportService reports) { this.reports = reports; }

    @GetMapping("/api/v1/projects/{projectId}/reports")
    public ProjectDashboardResponse dashboard(JwtAuthenticationToken auth, @PathVariable UUID projectId) {
        return reports.dashboard(userId(auth), projectId);
    }

    @GetMapping("/api/v1/projects/{projectId}/reports/sprints/{sprintId}")
    public SprintReportResponse sprintReport(JwtAuthenticationToken auth, @PathVariable UUID projectId,
            @PathVariable UUID sprintId) {
        return reports.sprintReport(userId(auth), projectId, sprintId);
    }

    private static UUID userId(JwtAuthenticationToken auth) { return UUID.fromString(auth.getToken().getSubject()); }
}
