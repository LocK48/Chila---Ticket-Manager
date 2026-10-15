package com.chila.backend.issue;

import java.util.UUID;

import com.chila.backend.project.ProjectStatus;
import com.chila.backend.workspace.MembershipRepository;
import org.springframework.stereotype.Service;

@Service
public class IssueAccessService {
    private final IssueRepository issueRepository;
    private final MembershipRepository membershipRepository;
    public IssueAccessService(IssueRepository issueRepository, MembershipRepository membershipRepository) {
        this.issueRepository = issueRepository; this.membershipRepository = membershipRepository;
    }
    public IssueEntity requireMemberIssue(UUID actorId, UUID issueId) {
        IssueEntity issue = issueRepository.findById(issueId).orElseThrow(IssueNotFoundException::new);
        if (!membershipRepository.existsByWorkspaceIdAndUserId(issue.getProject().getWorkspace().getId(), actorId)) {
            throw new IssueNotFoundException();
        }
        return issue;
    }
    public static void requireWritable(IssueEntity issue) {
        if (issue.getArchivedAt() != null) throw new IssueArchivedException();
        if (issue.getProject().getStatus() != ProjectStatus.ACTIVE) throw new ArchivedProjectException();
    }
}
