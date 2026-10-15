package com.chila.backend.notification;

import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    @Query(value = "select n from NotificationEntity n where n.recipient.id = :recipientId "
            + "and (:unreadOnly = false or n.readAt is null) "
            + "and (n.issue is null or exists (select m.id from MembershipEntity m "
            + "where m.workspace.id = n.issue.project.workspace.id and m.user.id = :recipientId)) "
            + "order by n.createdAt desc",
            countQuery = "select count(n) from NotificationEntity n where n.recipient.id = :recipientId "
                    + "and (:unreadOnly = false or n.readAt is null) "
                    + "and (n.issue is null or exists (select m.id from MembershipEntity m "
                    + "where m.workspace.id = n.issue.project.workspace.id and m.user.id = :recipientId))")
    Page<NotificationEntity> findVisibleByRecipient(@Param("recipientId") UUID recipientId,
            @Param("unreadOnly") boolean unreadOnly, Pageable pageable);
}
