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
import jakarta.annotation.PostConstruct;
import nl.martijndwars.web_push.Notification;
import nl.martijndwars.web_push.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final InAppNotificationRepository inAppNotificationRepository;
    private final UserRepository userRepository;

    @Value("${app.webpush.public-key:}")
    private String vapidPublicKey;

    @Value("${app.webpush.private-key:}")
    private String vapidPrivateKey;

    @Value("${app.webpush.subject:mailto:admin@myciti.com}")
    private String vapidSubject;

    private PushService pushService;

    public NotificationService(
        PushSubscriptionRepository pushSubscriptionRepository,
        InAppNotificationRepository inAppNotificationRepository,
        UserRepository userRepository
    ) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.inAppNotificationRepository = inAppNotificationRepository;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void initPushService() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        if (vapidPublicKey != null && !vapidPublicKey.isBlank() && vapidPrivateKey != null && !vapidPrivateKey.isBlank()) {
            try {
                this.pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
                log.info("[WebPush] PushService initialized successfully with VAPID subject: {}", vapidSubject);
            } catch (Exception e) {
                log.error("[WebPush] Failed to initialize PushService with VAPID keys", e);
            }
        } else {
            log.warn("[WebPush] VAPID keys not configured; background push delivery will be disabled.");
        }
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
            CompletableFuture.runAsync(() -> {
                try {
                    if (pushService != null && sub.getP256dh() != null && sub.getAuth() != null) {
                        Notification notification = new Notification(
                            sub.getEndpoint(),
                            sub.getP256dh(),
                            sub.getAuth(),
                            payload.getBytes(StandardCharsets.UTF_8)
                        );
                        HttpResponse response = pushService.send(notification);
                        int status = response.getStatusLine().getStatusCode();
                        if (status == 404 || status == 410) {
                            log.info("[WebPush] Pruning expired endpoint: {}", sub.getEndpoint());
                            pushSubscriptionRepository.deleteByEndpoint(sub.getEndpoint());
                        } else {
                            log.debug("[WebPush] Successfully pushed notification to user #{}, status: {}", userId, status);
                        }
                    }
                } catch (Exception e) {
                    log.warn("[WebPush] Failed push delivery to user #{}: {}", userId, e.getMessage());
                }
            });
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
