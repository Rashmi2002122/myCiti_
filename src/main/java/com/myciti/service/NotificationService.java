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
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // Register BouncyCastle for EC key operations required by Web Push ECDH
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        if (vapidPublicKey != null && !vapidPublicKey.isBlank()
                && vapidPrivateKey != null && !vapidPrivateKey.isBlank()) {
            try {
                this.pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
                log.info("[WebPush] PushService ready with VAPID subject: {}", vapidSubject);
            } catch (Exception e) {
                log.error("[WebPush] Failed to initialise PushService: {}", e.getMessage(), e);
            }
        } else {
            log.warn("[WebPush] VAPID keys not configured – background push disabled.");
        }
    }

    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    // --------------- Subscription management ---------------

    /**
     * Save or update a Web Push subscription atomically (upsert on endpoint).
     */
    @Transactional
    public void savePushSubscription(Long userId, PushSubscriptionRequest req) {
        if (req == null || req.getEndpoint() == null || req.getEndpoint().isBlank()) {
            return;
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        String endpoint = req.getEndpoint().trim();
        String p256dh   = req.getP256dh() != null ? req.getP256dh().trim() : "";
        String auth     = req.getAuth()    != null ? req.getAuth().trim()    : "";

        try {
            pushSubscriptionRepository.upsertSubscription(user.getId(), endpoint, p256dh, auth);
        } catch (Exception ex) {
            log.debug("[WebPush] Native upsert fallback: {}", ex.getMessage());
            pushSubscriptionRepository.findByEndpoint(endpoint).ifPresentOrElse(
                existing -> {
                    existing.setUser(user);
                    existing.setP256dh(p256dh);
                    existing.setAuth(auth);
                    pushSubscriptionRepository.save(existing);
                },
                () -> {
                    try {
                        pushSubscriptionRepository.save(new PushSubscription(user, endpoint, p256dh, auth));
                    } catch (Exception ignored) { /* concurrent insert already handled */ }
                }
            );
        }
    }

    @Transactional
    public void removePushSubscription(String endpoint) {
        if (endpoint != null && !endpoint.isBlank()) {
            pushSubscriptionRepository.deleteByEndpoint(endpoint);
        }
    }

    // --------------- In-App notification reads ---------------

    @Transactional(readOnly = true)
    public List<InAppNotificationDto> getUserNotifications(Long userId) {
        return inAppNotificationRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
            .map(n -> new InAppNotificationDto(n.getId(), n.getTitle(), n.getMessage(),
                                               n.getLinkUrl(), n.isRead(), n.getCreatedAt()))
            .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return inAppNotificationRepository.countByUser_IdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        inAppNotificationRepository.findByIdAndUser_Id(notificationId, userId).ifPresent(n -> {
            n.setRead(true);
            inAppNotificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        inAppNotificationRepository.markAllAsRead(userId);
    }

    // --------------- Dispatch ---------------

    /**
     * Persist in-app notification for each recipient AND fire a background
     * Web Push to every registered device via RFC 8291/8292 (ECDH + VAPID).
     */
    @Transactional
    public int dispatchNotificationToUsers(
            List<User> recipients, Announcement announcement,
            String title, String message, String linkUrl) {

        if (recipients == null || recipients.isEmpty()) return 0;

        int delivered = 0;
        for (User recipient : recipients) {
            InAppNotification notif = new InAppNotification(
                    recipient, announcement, title, message, linkUrl);
            inAppNotificationRepository.save(notif);
            delivered++;

            sendWebPushToUserDevices(recipient.getId(), title, message, linkUrl);
        }
        return delivered;
    }

    /**
     * Send a properly RFC-8291-encrypted Web Push notification to every device
     * the user has registered.  Uses nl.martijndwars:web-push which handles
     * ECDH key-agreement, AES-128-GCM content-encryption, and VAPID JWT signing.
     */
    private void sendWebPushToUserDevices(Long userId, String title, String message, String linkUrl) {
        if (pushService == null) {
            log.debug("[WebPush] PushService not initialised – skipping push for user #{}", userId);
            return;
        }

        List<PushSubscription> subs = pushSubscriptionRepository.findByUser_Id(userId);
        if (subs.isEmpty()) return;

        // Build JSON payload — service worker reads this in push-sw.js
        String payload = String.format(
            "{\"title\":\"%s\",\"body\":\"%s\",\"url\":\"%s\",\"icon\":\"/icon.svg\"}",
            escapeJson(title),
            escapeJson(message),
            linkUrl != null ? escapeJson(linkUrl) : "/"
        );

        for (PushSubscription sub : subs) {
            if (sub.getEndpoint() == null || sub.getP256dh() == null || sub.getAuth() == null) continue;

            CompletableFuture.runAsync(() -> {
                try {
                    Notification notification = new Notification(
                        sub.getEndpoint(),
                        sub.getP256dh(),
                        sub.getAuth(),
                        payload.getBytes()
                    );
                    org.apache.http.HttpResponse response = pushService.send(notification);
                    int status = response.getStatusLine().getStatusCode();

                    if (status == 404 || status == 410) {
                        log.info("[WebPush] Expired subscription, pruning: {}", sub.getEndpoint());
                        pushSubscriptionRepository.deleteByEndpoint(sub.getEndpoint());
                    } else if (status >= 200 && status < 300) {
                        log.debug("[WebPush] ✓ Delivered to user #{} ({})", userId, status);
                    } else {
                        log.warn("[WebPush] Push returned {} for user #{}", status, userId);
                    }
                } catch (Exception e) {
                    log.warn("[WebPush] Failed delivery to user #{}: {}", userId, e.getMessage());
                }
            });
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
