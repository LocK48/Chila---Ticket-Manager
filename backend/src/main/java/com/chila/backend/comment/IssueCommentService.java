package com.chila.backend.comment;

import java.util.UUID;

import com.chila.backend.activity.IssueActivityService;
import com.chila.backend.comment.api.CreateIssueCommentRequest;
import com.chila.backend.comment.api.IssueCommentPageResponse;
import com.chila.backend.comment.api.IssueCommentResponse;
import com.chila.backend.issue.IssueAccessService;
import com.chila.backend.issue.IssueEntity;
import com.chila.backend.notification.NotificationService;
import com.chila.backend.user.UserEntity;
import com.chila.backend.user.UserNotFoundException;
import com.chila.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class IssueCommentService {
    private final IssueCommentRepository comments;
    private final IssueAccessService access;
    private final UserRepository users;
    private final IssueActivityService activities;
    private final NotificationService notifications;
    public IssueCommentService(IssueCommentRepository comments, IssueAccessService access, UserRepository users,
            IssueActivityService activities, NotificationService notifications) {
        this.comments = comments; this.access = access; this.users = users;
        this.activities = activities; this.notifications = notifications;
    }
    @Transactional(readOnly = true)
    public IssueCommentPageResponse list(UUID actorId, UUID issueId, int page, int size) {
        IssueEntity issue = access.requireMemberIssue(actorId, issueId);
        Page<IssueCommentEntity> result = comments.findByIssueIdOrderByCreatedAtDesc(issue.getId(), PageRequest.of(page, size));
        return new IssueCommentPageResponse(result.getContent().stream().map(IssueCommentService::response).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
    @Transactional
    public IssueCommentResponse create(UUID actorId, UUID issueId, CreateIssueCommentRequest request) {
        IssueEntity issue = access.requireMemberIssue(actorId, issueId);
        IssueAccessService.requireWritable(issue);
        UserEntity author = users.findById(actorId).orElseThrow(UserNotFoundException::new);
        IssueCommentEntity comment = comments.save(new IssueCommentEntity(issue, author, request.body().trim()));
        activities.record(issue, author, "COMMENTED", null, null, comment.getBody());
        notifications.onComment(issue, author);
        return response(comment);
    }
    private static IssueCommentResponse response(IssueCommentEntity item) {
        return new IssueCommentResponse(item.getId(), item.getIssue().getId(), item.getAuthor().getId(),
                item.getAuthor().getDisplayName(), item.getBody(), item.getCreatedAt());
    }
}
