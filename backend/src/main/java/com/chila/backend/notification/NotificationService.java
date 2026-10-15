package com.chila.backend.notification;

import java.util.LinkedHashSet;
import java.util.UUID;

import com.chila.backend.issue.IssueEntity;
import com.chila.backend.workspace.MembershipRepository;
import com.chila.backend.user.UserEntity;
import com.chila.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chila.backend.notification.api.NotificationPageResponse;
import com.chila.backend.notification.api.NotificationResponse;

@Service
public class NotificationService {
    private final NotificationRepository repository;
    private final UserRepository users;
    private final MembershipRepository memberships;
    public NotificationService(NotificationRepository repository, UserRepository users, MembershipRepository memberships) {
        this.repository = repository; this.users = users; this.memberships = memberships;
    }
    @Transactional
    public void onAssignment(IssueEntity issue, UserEntity assignee, UUID actorId) {
        if (assignee != null && !assignee.getId().equals(actorId)
                && memberships.existsByWorkspaceIdAndUserId(issue.getProject().getWorkspace().getId(), assignee.getId())) {
            repository.save(new NotificationEntity(assignee, issue, "ISSUE_ASSIGNED",
                    "You were assigned to " + issue.getProject().getKey() + " · " + issue.getTitle()));
        }
    }
    @Transactional
    public void onComment(IssueEntity issue, UserEntity author) {
        LinkedHashSet<UUID> recipients = new LinkedHashSet<>();
        if (issue.getAssignee() != null) recipients.add(issue.getAssignee().getId());
        recipients.add(issue.getReporter().getId());
        recipients.remove(author.getId());
        recipients.removeIf(id -> !memberships.existsByWorkspaceIdAndUserId(issue.getProject().getWorkspace().getId(), id));
        String message = author.getDisplayName() + " commented on " + issue.getProject().getKey() + " · " + issue.getTitle();
        recipients.forEach(id -> users.findById(id).ifPresent(user -> repository.save(
                new NotificationEntity(user, issue, "ISSUE_COMMENTED", message))));
    }
    @Transactional(readOnly = true)
    public NotificationPageResponse list(UUID userId, int page, int size, boolean unreadOnly) {
        Page<NotificationEntity> result = repository.findVisibleByRecipient(userId, unreadOnly, PageRequest.of(page, size));
        return new NotificationPageResponse(result.getContent().stream().map(item -> new NotificationResponse(
                item.getId(), item.getIssue() == null ? null : item.getIssue().getId(),
                item.getIssue() == null ? null : item.getIssue().getTitle(), item.getEventType(), item.getMessage(),
                item.getCreatedAt(), item.getReadAt())).toList(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        NotificationEntity notification = repository.findById(notificationId)
                .filter(item -> item.getRecipient().getId().equals(userId)).orElseThrow(NotificationNotFoundException::new);
        if (notification.getIssue() != null && !memberships.existsByWorkspaceIdAndUserId(
                notification.getIssue().getProject().getWorkspace().getId(), userId)) {
            throw new NotificationNotFoundException();
        }
        notification.markRead();
        return new NotificationResponse(notification.getId(), notification.getIssue() == null ? null : notification.getIssue().getId(),
                notification.getIssue() == null ? null : notification.getIssue().getTitle(), notification.getEventType(),
                notification.getMessage(), notification.getCreatedAt(), notification.getReadAt());
    }
}
