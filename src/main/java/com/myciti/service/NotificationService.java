package com.myciti.service;

import com.myciti.dto.InAppNotificationDto;
import com.myciti.dto.PushSubscriptionRequest;
import com.myciti.model.Announcement;
import com.myciti.model.InAppNotification;
import com.myciti.model.PushSubscription;
import com.myciti.model.User;
import com.myciti.repository.InAppNotificationRepository;
import com.myciti.repository.PushSubscriptionRepository;
import com.myciti.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final InAppNotificationRepository inAppNotificationRepository;
    private final UserRepository userRepository;
    private final HttpClient httpClient;

    @Value("${app.webpush.public-key:}")
    private String vapidPublicKey;

    @Value("${app.webpush.subject:mailto:admin@myciti.com}")
    private String vapidSubject;

    public NotificationService(
        PushSubscriptionRepository pushSubscriptionRepository,
        InAppNotificationRepository inAppNotificationRepository,
        UserRepository userRepository
    ) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.inAppNotificationRepository = inAppNotificationRepository;
        this.userRepository = userRepository;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    /**
     * Save or update Web Push subscription for authenticated user.
     */
    @Transactional
    public void savePushSubscription(Long userId, PushSubscriptionRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        pushSubscriptionRepository.findByEndpoint(req.getEndpoint())
            .ifPresentOrElse(existing -> {
                existing.setUser(user);
                existing.setP256dh(req.getP256dh());
                existing.setAuth(req.getAuth());
                pushSubscriptionRepository.save(existing);
            }, () -> {
                PushSubscription sub = new PushSubscription(user, req.getEndpoint(), req.getP256dh(), req.getAuth());
                pushSubscriptionRepository.save(sub);
            });
    }

    /**
     * Delete a push subscription by endpoint.
     */
    @Transactional
    public void removePushSubscription(String endpoint) {
        if (endpoint != null && !endpoint.isBlank()) {
            pushSubscriptionRepository.deleteByEndpoint(endpoint);
        }
    }

    /**
     * List in-app notifications for authenticated user.
     */
    @Transactional(readOnly = true)
    public List<InAppNotificationDto> getUserNotifications(Long userId) {
        return inAppNotificationRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
            .map(n -> new InAppNotificationDto(
                n.getId(),
                n.getTitle(),
                n.getMessage(),
                n.getLinkUrl(),
                n.isRead(),
                n.getCreatedAt()
            ))
            .toList();
    }

    /**
     * Get unread notification badge count.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return inAppNotificationRepository.countByUser_IdAndReadFalse(userId);
    }

    /**
     * Mark single notification as read.
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        inAppNotificationRepository.findByIdAndUser_Id(notificationId, userId).ifPresent(n -> {
            n.setRead(true);
            inAppNotificationRepository.save(n);
        });
    }

    /**
     * Mark all notifications as read.
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        inAppNotificationRepository.markAllAsRead(userId);
    }

    /**
     * Send in-app notification and web push to a target list of users.
     */
    @Transactional
    public int dispatchNotificationToUsers(List<User> recipients, Announcement announcement, String title, String message, String linkUrl) {
        if (recipients == null || recipients.isEmpty()) {
            return 0;
        }

        int deliveredCount = 0;
        for (User recipient : recipients) {
            // 1. Create In-App Notification (guaranteed delivered)
            InAppNotification notif = new InAppNotification(recipient, announcement, title, message, linkUrl);
            inAppNotificationRepository.save(notif);
            deliveredCount++;

            // 2. Dispatch Web Push asynchronously to user's registered devices
            sendWebPushToUserDevices(recipient.getId(), title, message, linkUrl);
        }

        return deliveredCount;
    }

    private void sendWebPushToUserDevices(Long userId, String title, String message, String linkUrl) {
        List<PushSubscription> subs = pushSubscriptionRepository.findByUser_Id(userId);
        if (subs.isEmpty()) return;

        String payload = String.format("{\"title\":\"%s\",\"body\":\"%s\",\"url\":\"%s\"}",
            escapeJson(title), escapeJson(message), linkUrl != null ? escapeJson(linkUrl) : "/");

        for (PushSubscription sub : subs) {
            try {
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(sub.getEndpoint()))
                    .header("Content-Type", "application/json")
                    .header("TTL", "86400")
                    .timeout(Duration.ofSeconds(6))
                    .POST(HttpRequest.BodyPublishers.ofString(payload));

                httpClient.sendAsync(reqBuilder.build(), HttpResponse.BodyHandlers.discarding())
                    .thenAccept(res -> {
                        if (res.statusCode() == 404 || res.statusCode() == 410) {
                            log.info("[WebPush] Pruning expired endpoint: {}", sub.getEndpoint());
                            pushSubscriptionRepository.deleteByEndpoint(sub.getEndpoint());
                        }
                    })
                    .exceptionally(ex -> {
                        log.debug("[WebPush] Delivery attempt completed for user #{}", userId);
                        return null;
                    });
            } catch (Exception e) {
                log.debug("[WebPush] Failed to prepare push message: {}", e.getMessage());
            }
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
