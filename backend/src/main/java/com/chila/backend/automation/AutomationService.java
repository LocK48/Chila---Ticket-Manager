package com.chila.backend.automation;

import java.util.List;
import java.util.UUID;
import com.chila.backend.automation.api.*;
import com.chila.backend.common.api.InvalidInputException;
import com.chila.backend.issue.ArchivedProjectException;
import com.chila.backend.issue.IssueEntity;
import com.chila.backend.issue.IssueStatus;
import com.chila.backend.notification.NotificationEntity;
import com.chila.backend.notification.NotificationRepository;
import com.chila.backend.project.*;
import com.chila.backend.user.UserEntity;
import com.chila.backend.user.UserRepository;
import com.chila.backend.workspace.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutomationService {
    private static final int MAX_RULES_PER_PROJECT = 50;
    private final ProjectRepository projects;
    private final MembershipRepository memberships;
    private final UserRepository users;
    private final AutomationRuleRepository rules;
    private final AutomationExecutionRepository executions;
    private final NotificationRepository notifications;

    public AutomationService(ProjectRepository projects, MembershipRepository memberships, UserRepository users,
            AutomationRuleRepository rules, AutomationExecutionRepository executions, NotificationRepository notifications) {
        this.projects = projects; this.memberships = memberships; this.users = users;
        this.rules = rules; this.executions = executions; this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<AutomationRuleResponse> listRules(UUID actor, UUID projectId) {
        memberProject(actor, projectId);
        return rules.findByProject_IdOrderByCreatedAtDesc(projectId).stream().map(AutomationService::response).toList();
    }

    @Transactional
    public AutomationRuleResponse create(UUID actor, UUID projectId, AutomationRuleRequest request) {
        ProjectEntity project = managerProject(actor, projectId);
        if (rules.countByProject_Id(projectId) >= MAX_RULES_PER_PROJECT)
            throw new InvalidInputException("rules", "A project can have at most 50 automation rules.");
        UserEntity creator = users.findById(actor).orElseThrow(ProjectNotFoundException::new);
        AutomationRuleEntity rule = rules.save(new AutomationRuleEntity(project, creator, request.name().trim(),
                request.enabled(), request.targetStatus(), request.action()));
        return response(rule);
    }

    @Transactional
    public AutomationRuleResponse update(UUID actor, UUID projectId, UUID ruleId, AutomationRuleRequest request) {
        managerProject(actor, projectId);
        AutomationRuleEntity rule = rules.findByIdAndProject_Id(ruleId, projectId).orElseThrow(ProjectNotFoundException::new);
        rule.update(request.name().trim(), request.enabled(), request.targetStatus(), request.action());
        return response(rule);
    }

    @Transactional
    public void delete(UUID actor, UUID projectId, UUID ruleId) {
        managerProject(actor, projectId);
        AutomationRuleEntity rule = rules.findByIdAndProject_Id(ruleId, projectId).orElseThrow(ProjectNotFoundException::new);
        rules.delete(rule);
    }

    @Transactional(readOnly = true)
    public List<AutomationExecutionResponse> listExecutions(UUID actor, UUID projectId) {
        memberProject(actor, projectId);
        return executions.findByProject_IdOrderByExecutedAtDesc(projectId, PageRequest.of(0, 50)).stream()
                .map(AutomationService::executionResponse).toList();
    }

    @Transactional
    public void issueStatusChanged(IssueEntity issue) {
        List<AutomationRuleEntity> matching = rules.findByProject_IdAndEnabledTrueAndTargetStatusOrderByCreatedAtAsc(
                issue.getProject().getId(), issue.getStatus());
        for (AutomationRuleEntity rule : matching) {
            UserEntity recipient = switch (rule.getAction()) {
                case NOTIFY_REPORTER -> issue.getReporter();
                case NOTIFY_ASSIGNEE -> issue.getAssignee();
            };
            if (recipient == null || !memberships.existsByWorkspaceIdAndUserId(
                    issue.getProject().getWorkspace().getId(), recipient.getId())) {
                executions.save(new AutomationExecutionEntity(rule, issue, AutomationOutcome.SKIPPED,
                        recipient == null ? "No recipient matched the selected action." : "Recipient is no longer a workspace member."));
                continue;
            }
            String issueKey = issue.getProject().getKey() + "-" + issue.getIssueNumber();
            notifications.save(new NotificationEntity(recipient, issue, "AUTOMATION",
                    "Automation ‘" + rule.getName() + "’ matched " + issueKey + ": " + issue.getTitle()));
            executions.save(new AutomationExecutionEntity(rule, issue, AutomationOutcome.SUCCEEDED,
                    "Created a notification for " + rule.getAction().name().toLowerCase().replace('_', ' ') + "."));
        }
    }

    private ProjectEntity memberProject(UUID actor, UUID projectId) {
        ProjectEntity project = projects.findById(projectId).orElseThrow(ProjectNotFoundException::new);
        if (!memberships.existsByWorkspaceIdAndUserId(project.getWorkspace().getId(), actor))
            throw new ProjectNotFoundException();
        return project;
    }

    private ProjectEntity managerProject(UUID actor, UUID projectId) {
        ProjectEntity project = memberProject(actor, projectId);
        MembershipRole role = memberships.findByWorkspaceIdAndUserId(project.getWorkspace().getId(), actor)
                .orElseThrow(ProjectNotFoundException::new).getRole();
        if (role == MembershipRole.MEMBER) throw new WorkspaceAccessDeniedException();
        if (project.getStatus() != ProjectStatus.ACTIVE) throw new ArchivedProjectException();
        return project;
    }

    private static AutomationRuleResponse response(AutomationRuleEntity rule) {
        return new AutomationRuleResponse(rule.getId(), rule.getProject().getId(), rule.getName(), rule.isEnabled(),
                rule.getTargetStatus(), rule.getAction(), rule.getCreatedAt(), rule.getUpdatedAt());
    }

    private static AutomationExecutionResponse executionResponse(AutomationExecutionEntity execution) {
        return new AutomationExecutionResponse(execution.getId(), execution.getRuleId(), execution.getRuleName(),
                execution.getIssueKey(), execution.getOutcome(), execution.getDetail(), execution.getExecutedAt());
    }
}
