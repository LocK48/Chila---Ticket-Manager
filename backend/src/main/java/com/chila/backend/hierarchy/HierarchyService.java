package com.chila.backend.hierarchy;

import java.time.LocalDate;
import java.util.*;
import com.chila.backend.common.api.InvalidInputException;
import com.chila.backend.activity.IssueActivityService;
import com.chila.backend.hierarchy.api.*;
import com.chila.backend.issue.*;
import com.chila.backend.issue.api.IssuePageResponse;
import com.chila.backend.issue.api.IssueResponse;
import com.chila.backend.project.*;
import com.chila.backend.user.*;
import com.chila.backend.workspace.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HierarchyService {
    private final EpicRepository epics;
    private final ReleaseRepository releases;
    private final IssueRepository issues;
    private final IssueLinkRepository links;
    private final ProjectRepository projects;
    private final MembershipRepository memberships;
    private final UserRepository users;
    private final IssueActivityService activity;

    public HierarchyService(EpicRepository epics, ReleaseRepository releases, IssueRepository issues,
            IssueLinkRepository links, ProjectRepository projects, MembershipRepository memberships,
            UserRepository users, IssueActivityService activity) {
        this.epics = epics; this.releases = releases; this.issues = issues; this.links = links;
        this.projects = projects; this.memberships = memberships; this.users = users; this.activity = activity;
    }

    @Transactional(readOnly = true)
    public List<EpicResponse> listEpics(UUID actor, UUID projectId) {
        ProjectEntity project = memberProject(actor, projectId);
        return epics.findByProject_IdOrderByNameAsc(project.getId()).stream().map(this::epicResponse).toList();
    }

    @Transactional
    public EpicResponse createEpic(UUID actor, UUID projectId, EpicRequest request) {
        ProjectEntity project = managerProject(actor, projectId);
        String name = request.name().trim();
        if (epics.existsByProject_IdAndNameIgnoreCase(projectId, name)) throw new InvalidInputException("name", "An epic with this name already exists.");
        EpicEntity epic = epics.save(new EpicEntity(project, name, normalize(request.summary()),
                request.color() == null ? "#6554C0" : request.color().toUpperCase(Locale.ROOT)));
        return epicResponse(epic);
    }

    @Transactional
    public EpicResponse updateEpic(UUID actor, UUID projectId, UUID epicId, EpicRequest request) {
        managerProject(actor, projectId);
        EpicEntity epic = epics.findByIdAndProject_Id(epicId, projectId).orElseThrow(ProjectNotFoundException::new);
        String name = request.name().trim();
        if (!name.equalsIgnoreCase(epic.getName()) && epics.existsByProject_IdAndNameIgnoreCase(projectId, name)) {
            throw new InvalidInputException("name", "An epic with this name already exists.");
        }
        epic.update(name, normalize(request.summary()), request.color() == null ? epic.getColor() : request.color().toUpperCase(Locale.ROOT));
        return epicResponse(epic);
    }

    @Transactional(readOnly = true)
    public IssuePageResponse epicIssues(UUID actor, UUID projectId, UUID epicId, IssueStatus status, String search, int page, int size) {
        memberProject(actor, projectId);
        epics.findByIdAndProject_Id(epicId, projectId).orElseThrow(ProjectNotFoundException::new);
        validatePage(page, size);
        Page<IssueEntity> result = issues.searchEpicIssues(projectId, epicId, status,
                StringUtils.hasText(search) ? "%" + search.trim().toLowerCase(Locale.ROOT) + "%" : "%",
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "rankValue").and(Sort.by("id"))));
        return new IssuePageResponse(result.getContent().stream().map(IssueService::response).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<ReleaseResponse> listReleases(UUID actor, UUID projectId) {
        memberProject(actor, projectId);
        return releases.findByProject_IdOrderByStartDateAscReleaseDateAscNameAsc(projectId).stream().map(this::releaseResponse).toList();
    }

    @Transactional
    public ReleaseResponse createRelease(UUID actor, UUID projectId, ReleaseRequest request) {
        ProjectEntity project = managerProject(actor, projectId);
        validateDates(request.startDate(), request.releaseDate());
        String name = request.name().trim();
        if (releases.existsByProject_IdAndNameIgnoreCase(projectId, name)) throw new InvalidInputException("name", "A release with this name already exists.");
        ReleaseEntity release = new ReleaseEntity(project, name, normalize(request.description()), request.startDate(), request.releaseDate());
        if (request.state() != null) release.setState(request.state());
        return releaseResponse(releases.save(release));
    }

    @Transactional
    public ReleaseResponse updateRelease(UUID actor, UUID projectId, UUID releaseId, ReleaseRequest request) {
        managerProject(actor, projectId);
        validateDates(request.startDate(), request.releaseDate());
        ReleaseEntity release = releases.findByIdAndProject_Id(releaseId, projectId).orElseThrow(ProjectNotFoundException::new);
        String name = request.name().trim();
        if (!name.equalsIgnoreCase(release.getName()) && releases.existsByProject_IdAndNameIgnoreCase(projectId, name)) {
            throw new InvalidInputException("name", "A release with this name already exists.");
        }
        release.update(name, normalize(request.description()), request.startDate(), request.releaseDate());
        if (request.state() != null) release.setState(request.state());
        return releaseResponse(release);
    }

    @Transactional
    public IssueResponse assignEpic(UUID actor, UUID issueId, IdAssignmentRequest request) {
        IssueEntity issue = writableIssue(actor, issueId);
        EpicEntity epic = request.id() == null ? null : epics.findByIdAndProject_Id(request.id(), issue.getProject().getId())
                .orElseThrow(ProjectNotFoundException::new);
        String oldValue = issue.getEpic() == null ? null : issue.getEpic().getName();
        issue.assignEpic(epic);
        record(actor, issue, "epic", oldValue, epic == null ? null : epic.getName());
        return IssueService.response(issue);
    }

    @Transactional
    public IssueResponse assignRelease(UUID actor, UUID issueId, IdAssignmentRequest request) {
        IssueEntity issue = writableIssue(actor, issueId);
        ReleaseEntity release = request.id() == null ? null : releases.findByIdAndProject_Id(request.id(), issue.getProject().getId())
                .orElseThrow(ProjectNotFoundException::new);
        if (release != null && release.getState() == ReleaseState.ARCHIVED) throw new InvalidInputException("id", "Issues cannot be added to an archived release.");
        String oldValue = issue.getRelease() == null ? null : issue.getRelease().getName();
        issue.assignRelease(release);
        record(actor, issue, "release", oldValue, release == null ? null : release.getName());
        return IssueService.response(issue);
    }

    @Transactional
    public IssueResponse setParent(UUID actor, UUID issueId, IdAssignmentRequest request) {
        IssueEntity issue = writableIssue(actor, issueId);
        ProjectEntity project = projects.findByIdForUpdate(issue.getProject().getId()).orElseThrow(ProjectNotFoundException::new);
        if (project.getStatus() != ProjectStatus.ACTIVE) throw new ArchivedProjectException();
        IssueEntity parent = request.id() == null ? null : issues.findById(request.id()).orElseThrow(IssueNotFoundException::new);
        if (parent != null) {
            writableIssue(actor, parent.getId());
            if (!parent.getProject().getId().equals(issue.getProject().getId())) throw new IssueNotFoundException();
            if (parent.getId().equals(issue.getId())) throw new InvalidInputException("id", "An issue cannot be its own parent.");
            IssueEntity cursor = parent;
            Set<UUID> visited = new HashSet<>();
            while (cursor != null && visited.add(cursor.getId())) {
                if (cursor.getId().equals(issue.getId())) throw new InvalidInputException("id", "This parent would create a hierarchy cycle.");
                cursor = cursor.getParentIssue();
            }
        }
        String oldValue = issue.getParentIssue() == null ? null : issueKey(issue.getParentIssue());
        issue.setParentIssue(parent);
        record(actor, issue, "parent", oldValue, parent == null ? null : issueKey(parent));
        return IssueService.response(issue);
    }

    @Transactional
    public IssueLinkResponse addLink(UUID actor, UUID issueId, IssueLinkRequest request) {
        IssueEntity source = writableIssue(actor, issueId);
        IssueEntity target = writableIssue(actor, request.targetIssueId());
        if (!source.getProject().getId().equals(target.getProject().getId())) throw new IssueNotFoundException();
        if (source.getId().equals(target.getId())) throw new InvalidInputException("targetIssueId", "An issue cannot link to itself.");
        if (links.existsBySource_IdAndTarget_IdAndType(source.getId(), target.getId(), request.type())) {
            throw new InvalidInputException("targetIssueId", "This issue link already exists.");
        }
        UserEntity user = users.findById(actor).orElseThrow(UserNotFoundException::new);
        IssueLinkEntity saved = links.save(new IssueLinkEntity(source, target, request.type(), user));
        record(actor, source, "issue link", null, request.type().name() + " " + issueKey(target));
        return linkResponse(saved);
    }

    @Transactional
    public void removeLink(UUID actor, UUID issueId, UUID linkId) {
        IssueEntity issue = writableIssue(actor, issueId);
        IssueLinkEntity link = links.findById(linkId).orElseThrow(IssueNotFoundException::new);
        if (!link.getSource().getId().equals(issueId) && !link.getTarget().getId().equals(issueId)) throw new IssueNotFoundException();
        writableIssue(actor, link.getSource().getId());
        links.delete(link);
        record(actor, issue, "issue link", link.getType().name(), null);
    }

    @Transactional(readOnly = true)
    public IssueRelationshipsResponse relationships(UUID actor, UUID issueId) {
        IssueEntity issue = memberIssue(actor, issueId);
        RelatedIssueSummary parent = issue.getParentIssue() == null ? null : summary(issue.getParentIssue());
        List<RelatedIssueSummary> children = issues.findByParentIssue_IdAndArchivedAtIsNullOrderByCreatedAtAsc(issueId, PageRequest.of(0, 100))
                .stream().map(HierarchyService::summary).toList();
        List<IssueLinkResponse> relatedLinks = links.findBySource_IdOrTarget_IdOrderByCreatedAtDesc(issueId, issueId)
                .stream().map(this::linkResponse).toList();
        return new IssueRelationshipsResponse(parent, children, relatedLinks);
    }

    private EpicResponse epicResponse(EpicEntity epic) {
        long total = issues.countActiveByEpicId(epic.getId());
        return new EpicResponse(epic.getId(), epic.getProject().getId(), epic.getName(), epic.getSummary(), epic.getColor(),
                total, issues.countDoneByEpicId(epic.getId()), epic.getCreatedAt(), epic.getUpdatedAt());
    }
    private ReleaseResponse releaseResponse(ReleaseEntity release) {
        return new ReleaseResponse(release.getId(), release.getProject().getId(), release.getName(), release.getDescription(),
                release.getStartDate(), release.getReleaseDate(), release.getState(), issues.countActiveByReleaseId(release.getId()),
                release.getCreatedAt(), release.getUpdatedAt());
    }
    private IssueLinkResponse linkResponse(IssueLinkEntity link) {
        return new IssueLinkResponse(link.getId(), link.getSource().getId(), issueKey(link.getSource()), link.getSource().getTitle(),
                link.getTarget().getId(), issueKey(link.getTarget()), link.getTarget().getTitle(), link.getType(), link.getCreatedAt());
    }
    private ProjectEntity memberProject(UUID actor, UUID projectId) {
        ProjectEntity project = projects.findById(projectId).orElseThrow(ProjectNotFoundException::new);
        if (!memberships.existsByWorkspaceIdAndUserId(project.getWorkspace().getId(), actor)) throw new WorkspaceNotFoundException();
        return project;
    }
    private ProjectEntity managerProject(UUID actor, UUID projectId) {
        ProjectEntity project = memberProject(actor, projectId);
        MembershipRole role = memberships.findByWorkspaceIdAndUserId(project.getWorkspace().getId(), actor).orElseThrow(WorkspaceNotFoundException::new).getRole();
        if (role == MembershipRole.MEMBER) throw new WorkspaceAccessDeniedException();
        if (project.getStatus() != ProjectStatus.ACTIVE) throw new ArchivedProjectException();
        return project;
    }
    private IssueEntity memberIssue(UUID actor, UUID issueId) {
        IssueEntity issue = issues.findById(issueId).orElseThrow(IssueNotFoundException::new);
        memberProject(actor, issue.getProject().getId());
        return issue;
    }
    private IssueEntity writableIssue(UUID actor, UUID issueId) {
        IssueEntity issue = memberIssue(actor, issueId);
        IssueAccessService.requireWritable(issue);
        return issue;
    }
    private void record(UUID actor, IssueEntity issue, String field, String oldValue, String newValue) {
        UserEntity user = users.findById(actor).orElseThrow(UserNotFoundException::new);
        activity.record(issue, user, "UPDATED", field, oldValue, newValue);
    }
    private static RelatedIssueSummary summary(IssueEntity issue) { return new RelatedIssueSummary(issue.getId(), issueKey(issue), issue.getTitle(), issue.getStatus().name()); }
    private static String issueKey(IssueEntity issue) { return issue.getProject().getKey() + "-" + issue.getIssueNumber(); }
    private static String normalize(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
    private static void validatePage(int page, int size) {
        if (page < 0) throw new InvalidInputException("page", "Page must be zero or greater.");
        if (size < 1 || size > 100) throw new InvalidInputException("size", "Page size must be between 1 and 100.");
    }
    private static void validateDates(LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) throw new InvalidInputException("releaseDate", "Release date must be on or after the start date.");
    }

}
