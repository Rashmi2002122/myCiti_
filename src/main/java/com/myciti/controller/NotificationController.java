package com.myciti.controller;

import com.myciti.dto.InAppNotificationDto;
import com.myciti.dto.PushSubscriptionRequest;
import com.myciti.model.User;
import com.myciti.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/vapid-public-key")
    public ResponseEntity<Map<String, String>> getVapidPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", notificationService.getVapidPublicKey() != null ? notificationService.getVapidPublicKey() : ""));
    }

    @PostMapping({"/subscribe", "/push-subscribe"})
    public ResponseEntity<Map<String, String>> subscribe(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody PushSubscriptionRequest req
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        notificationService.savePushSubscription(user.getId(), req);
        return ResponseEntity.ok(Map.of("message", "Subscribed successfully"));
    }

    @DeleteMapping("/unsubscribe")
    public ResponseEntity<Map<String, String>> unsubscribe(
        @AuthenticationPrincipal User user,
        @RequestParam String endpoint
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        notificationService.removePushSubscription(user.getId(), endpoint);
        return ResponseEntity.ok(Map.of("message", "Unsubscribed successfully"));
    }

    @GetMapping
    public ResponseEntity<List<InAppNotificationDto>> getMyNotifications(
        @AuthenticationPrincipal User user
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(notificationService.getUserNotifications(user.getId()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
        @AuthenticationPrincipal User user
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount(user.getId())));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
        @AuthenticationPrincipal User user,
        @PathVariable Long id
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
        @AuthenticationPrincipal User user
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
        @AuthenticationPrincipal User user,
        @PathVariable Long id
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        notificationService.deleteNotification(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/clear-all")
    public ResponseEntity<Void> clearAllNotifications(
        @AuthenticationPrincipal User user
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        notificationService.clearAllNotifications(user.getId());
        return ResponseEntity.noContent().build();
    }
}
