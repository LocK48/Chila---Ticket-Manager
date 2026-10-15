package com.chila.backend.activity;

import java.util.UUID;

import com.chila.backend.activity.api.IssueActivityPageResponse;
import com.chila.backend.activity.api.IssueActivityResponse;
import com.chila.backend.issue.IssueAccessService;
import com.chila.backend.issue.IssueEntity;
import com.chila.backend.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IssueActivityService {
    private final IssueActivityRepository repository;
    private final IssueAccessService access;
    public IssueActivityService(IssueActivityRepository repository, IssueAccessService access) {
        this.repository = repository; this.access = access;
    }
    @Transactional
    public void record(IssueEntity issue, UserEntity actor, String action, String field, String oldValue, String newValue) {
        repository.save(new IssueActivityEntity(issue, actor, action, field, safe(oldValue), safe(newValue)));
    }
    @Transactional(readOnly = true)
    public IssueActivityPageResponse list(UUID actorId, UUID issueId, int page, int size) {
        IssueEntity issue = access.requireMemberIssue(actorId, issueId);
        Page<IssueActivityEntity> result = repository.findByIssueIdOrderByCreatedAtDesc(issue.getId(), PageRequest.of(page, size));
        return new IssueActivityPageResponse(result.getContent().stream().map(item -> new IssueActivityResponse(
                item.getId(), issueId, item.getActor().getId(), item.getActor().getDisplayName(), item.getAction(),
                item.getFieldName(), item.getOldValue(), item.getNewValue(), item.getCreatedAt())).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
    private static String safe(String value) { return value == null ? null : value.substring(0, Math.min(value.length(), 2000)); }
}
