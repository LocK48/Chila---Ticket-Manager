package com.chila.backend.notification;

import java.util.UUID;

import com.chila.backend.activity.IssueActivityController;
import com.chila.backend.notification.api.NotificationPageResponse;
import com.chila.backend.notification.api.NotificationResponse;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }
    @GetMapping("/api/v1/users/me/notifications")
    public NotificationPageResponse list(JwtAuthenticationToken auth,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        IssueActivityController.validate(page, size);
        return service.list(userId(auth), page, size, unreadOnly);
    }
    @PatchMapping("/api/v1/notifications/{notificationId}/read")
    public NotificationResponse markRead(JwtAuthenticationToken auth, @PathVariable UUID notificationId) {
        return service.markRead(userId(auth), notificationId);
    }
    private static UUID userId(JwtAuthenticationToken auth) { return UUID.fromString(auth.getToken().getSubject()); }
}
