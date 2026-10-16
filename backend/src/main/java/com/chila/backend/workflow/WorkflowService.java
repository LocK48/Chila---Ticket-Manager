package com.chila.backend.workflow;

import java.util.*;
import com.chila.backend.common.api.InvalidInputException;
import com.chila.backend.issue.IssueStatus;
import com.chila.backend.issue.ArchivedProjectException;
import com.chila.backend.project.*;
import com.chila.backend.user.UserEntity;
import com.chila.backend.workspace.*;
import com.chila.backend.workflow.api.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowService {
    private static final List<WorkflowTransitionInput> DEFAULTS = List.of(
            new WorkflowTransitionInput(IssueStatus.TODO, IssueStatus.IN_PROGRESS),
            new WorkflowTransitionInput(IssueStatus.IN_PROGRESS, IssueStatus.TODO),
            new WorkflowTransitionInput(IssueStatus.IN_PROGRESS, IssueStatus.IN_REVIEW),
            new WorkflowTransitionInput(IssueStatus.IN_REVIEW, IssueStatus.IN_PROGRESS),
            new WorkflowTransitionInput(IssueStatus.IN_REVIEW, IssueStatus.DONE),
            new WorkflowTransitionInput(IssueStatus.DONE, IssueStatus.IN_PROGRESS));
    private final ProjectRepository projects;
    private final MembershipRepository memberships;
    private final WorkflowTransitionRepository transitions;
    private final WorkflowConfigRepository configs;

    public WorkflowService(ProjectRepository projects, MembershipRepository memberships, WorkflowTransitionRepository transitions, WorkflowConfigRepository configs) {
        this.projects = projects; this.memberships = memberships; this.transitions = transitions; this.configs = configs;
    }

    @Transactional(readOnly = true)
    public WorkflowResponse get(UUID actor, UUID projectId) {
        ProjectEntity project = memberProject(actor, projectId);
        List<WorkflowTransitionInput> allowed = transitions.findByProject_IdOrderByFromStatusAscToStatusAsc(projectId)
                .stream().map(t -> new WorkflowTransitionInput(t.getFromStatus(), t.getToStatus())).toList();
        if (!configs.existsById(projectId)) allowed = DEFAULTS;
        return new WorkflowResponse(List.of(IssueStatus.values()), allowed);
    }

    @Transactional
    public WorkflowResponse update(UUID actor, UUID projectId, List<WorkflowTransitionInput> input) {
        managerProject(actor, projectId);
        ProjectEntity project = projects.findByIdForUpdate(projectId).orElseThrow(ProjectNotFoundException::new);
        if (input.size() > 12) throw new InvalidInputException("transitions", "At most 12 workflow transitions are supported.");
        if (!configs.existsById(projectId)) configs.save(new WorkflowConfigEntity(project));
        Set<String> unique = new HashSet<>();
        for (WorkflowTransitionInput item : input) {
            if (item == null) throw new InvalidInputException("transitions", "Workflow transitions cannot contain null entries.");
            if (item.from() == item.to()) throw new InvalidInputException("transitions", "A status cannot transition to itself.");
            if (!unique.add(item.from() + ":" + item.to())) throw new InvalidInputException("transitions", "Duplicate status transition.");
        }
        transitions.deleteByProject_Id(projectId);
        transitions.saveAll(input.stream().map(t -> new WorkflowTransitionEntity(project, t.from(), t.to())).toList());
        return new WorkflowResponse(List.of(IssueStatus.values()), List.copyOf(input));
    }

    @Transactional(readOnly = true)
    public void requireAllowed(UUID projectId, IssueStatus from, IssueStatus to) {
        List<WorkflowTransitionEntity> configured = transitions.findByProject_IdOrderByFromStatusAscToStatusAsc(projectId);
        if (!configs.existsById(projectId)) {
            if (DEFAULTS.stream().noneMatch(t -> t.from() == from && t.to() == to)) throw new com.chila.backend.issue.InvalidStatusTransitionException();
        } else if (configured.stream().noneMatch(t -> t.getFromStatus() == from && t.getToStatus() == to)) {
            throw new com.chila.backend.issue.InvalidStatusTransitionException();
        }
    }

    private ProjectEntity memberProject(UUID actor, UUID id) {
        ProjectEntity p = projects.findById(id).orElseThrow(ProjectNotFoundException::new);
        if (!memberships.existsByWorkspaceIdAndUserId(p.getWorkspace().getId(), actor)) throw new ProjectNotFoundException();
        return p;
    }
    private ProjectEntity managerProject(UUID actor, UUID id) {
        ProjectEntity p = memberProject(actor, id);
        MembershipRole role = memberships.findByWorkspaceIdAndUserId(p.getWorkspace().getId(), actor).orElseThrow().getRole();
        if (role == MembershipRole.MEMBER) throw new WorkspaceAccessDeniedException();
        if (p.getStatus() != ProjectStatus.ACTIVE) throw new ArchivedProjectException();
        return p;
    }
}
