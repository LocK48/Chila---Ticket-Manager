package com.chila.backend.filter;

import java.util.*;
import com.chila.backend.common.api.InvalidInputException;
import com.chila.backend.filter.api.*;
import com.chila.backend.project.*;
import com.chila.backend.sprint.SprintRepository;
import com.chila.backend.hierarchy.EpicRepository;
import com.chila.backend.user.*;
import com.chila.backend.workspace.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SavedFilterService {
    private final SavedFilterRepository filters; private final ProjectRepository projects; private final MembershipRepository memberships; private final UserRepository users; private final SprintRepository sprints; private final EpicRepository epics;
    public SavedFilterService(SavedFilterRepository filters, ProjectRepository projects, MembershipRepository memberships, UserRepository users, SprintRepository sprints, EpicRepository epics) { this.filters=filters;this.projects=projects;this.memberships=memberships;this.users=users;this.sprints=sprints;this.epics=epics; }
    @Transactional(readOnly=true)
    public List<SavedFilterResponse> list(UUID actor, UUID projectId) {
        memberProject(actor,projectId);
        Map<UUID,SavedFilterEntity> visible=new LinkedHashMap<>();
        filters.findByProject_IdAndVisibilityOrderByNameAsc(projectId,SavedFilterVisibility.WORKSPACE).forEach(f->visible.put(f.getId(),f));
        filters.findByProject_IdAndCreator_IdOrderByNameAsc(projectId,actor).forEach(f->visible.put(f.getId(),f));
        return visible.values().stream().map(SavedFilterService::response).toList();
    }
    @Transactional
    public SavedFilterResponse create(UUID actor, UUID projectId, SavedFilterRequest request) {
        ProjectEntity project=memberProject(actor,projectId); validate(project,request);
        UserEntity creator=users.findById(actor).orElseThrow(UserNotFoundException::new);
        return response(filters.save(new SavedFilterEntity(project,creator,request.name().trim(),request.visibility(),request.status(),request.priority(),request.type(),request.assigneeId(),request.unassigned(),request.sprintId(),request.epicId(),request.dueFrom(),request.dueTo(),normalize(request.search()))));
    }
    @Transactional
    public SavedFilterResponse update(UUID actor,UUID projectId,UUID id,SavedFilterRequest request) {
        ProjectEntity project=memberProject(actor,projectId); validate(project,request);
        SavedFilterEntity f=filters.findByIdAndProject_Id(id,projectId).orElseThrow(ProjectNotFoundException::new);
        if (!f.getCreatorId().equals(actor)) throw new WorkspaceAccessDeniedException();
        f.update(request.name().trim(),request.visibility(),request.status(),request.priority(),request.type(),request.assigneeId(),request.unassigned(),request.sprintId(),request.epicId(),request.dueFrom(),request.dueTo(),normalize(request.search()));
        return response(f);
    }
    @Transactional
    public void delete(UUID actor,UUID projectId,UUID id) {
        memberProject(actor,projectId); SavedFilterEntity f=filters.findByIdAndProject_Id(id,projectId).orElseThrow(ProjectNotFoundException::new);
        if (!f.getCreatorId().equals(actor)) throw new WorkspaceAccessDeniedException(); filters.delete(f);
    }
    private void validate(ProjectEntity project,SavedFilterRequest r) {
        if (r.unassigned() && r.assigneeId()!=null) throw new InvalidInputException("assigneeId","Choose a member or unassigned issues, not both.");
        if (r.dueFrom()!=null && r.dueTo()!=null && r.dueFrom().isAfter(r.dueTo())) throw new InvalidInputException("dueTo","Due date end must be on or after start.");
        UUID projectId=project.getId();
        if (r.assigneeId()!=null && !memberships.existsByWorkspaceIdAndUserId(project.getWorkspace().getId(),r.assigneeId())) throw new InvalidInputException("assigneeId","Assignee must be a member of this workspace.");
        if (r.sprintId()!=null && sprints.findByIdAndProject_Id(r.sprintId(),projectId).isEmpty()) throw new InvalidInputException("sprintId","Choose a sprint from this project.");
        if (r.epicId()!=null && epics.findByIdAndProject_Id(r.epicId(),projectId).isEmpty()) throw new InvalidInputException("epicId","Choose an epic from this project.");
    }
    private ProjectEntity memberProject(UUID actor,UUID id) { ProjectEntity p=projects.findById(id).orElseThrow(ProjectNotFoundException::new);if(!memberships.existsByWorkspaceIdAndUserId(p.getWorkspace().getId(),actor))throw new ProjectNotFoundException();return p; }
    private static String normalize(String s){return StringUtils.hasText(s)?s.trim():null;}
    private static SavedFilterResponse response(SavedFilterEntity f){return new SavedFilterResponse(f.getId(),f.getProjectId(),f.getCreatorId(),f.getName(),f.getVisibility(),f.getStatus(),f.getPriority(),f.getType(),f.getAssigneeId(),f.isUnassigned(),f.getSprintId(),f.getEpicId(),f.getDueFrom(),f.getDueTo(),f.getSearch(),f.getCreatedAt(),f.getUpdatedAt());}
}
