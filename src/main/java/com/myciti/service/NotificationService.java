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
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private static final List<String> ALLOWED_PUSH_DOMAINS = List.of(
        "googleapis.com",
        "google.com",
        "mozilla.com",
        "mozilla.org",
        "push.apple.com",
        "notify.windows.com"
    );

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
    public void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        if (vapidPublicKey == null || vapidPublicKey.isBlank() || vapidPrivateKey == null || vapidPrivateKey.isBlank()) {
            log.warn("[WebPush] Web Push keys not configured. PushService disabled.");
            return;
        }

        String pub = vapidPublicKey.trim();
        String priv = vapidPrivateKey.trim();
        String subject = (vapidSubject != null && !vapidSubject.isBlank()) ? vapidSubject.trim() : "mailto:admin@myciti.com";

        try {
            this.pushService = new PushService(pub, priv, subject);
            this.vapidPublicKey = pub;
            log.info("[WebPush] Initialized RFC 8292 PushService with VAPID credentials.");
        } catch (Exception e) {
            log.warn("[WebPush] Failed to initialize PushService: {}. Web Push will be disabled.", e.getMessage());
        }
    }

    public String getVapidPublicKey() {
        return (vapidPublicKey != null && !vapidPublicKey.isBlank()) ? vapidPublicKey.trim() : "";
    }

    /**
     * SSRF defense: ensure the push endpoint strictly uses HTTPS, has a valid host,
     * is not a local/private IP or cloud metadata service, and belongs to an authorized push provider.
     */
    public static boolean isValidPushEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank() || endpoint.length() > 2048) {
            return false;
        }
        try {
            URI uri = URI.create(endpoint.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return false;
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }
            host = host.toLowerCase();

            // Disallow loopback, link-local, private LAN or cloud metadata IPs
            if (host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1")
                || host.startsWith("10.") || host.startsWith("192.168.")
                || host.startsWith("169.254.") || host.endsWith(".internal")
                || host.endsWith(".local")) {
                return false;
            }

            // Must match trusted push gateway domain suffixes
            for (String allowed : ALLOWED_PUSH_DOMAINS) {
                if (host.equals(allowed) || host.endsWith("." + allowed)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Save or update Web Push subscription for authenticated user.
     */
    @Transactional
    public void savePushSubscription(Long userId, PushSubscriptionRequest req) {
        if (!isValidPushEndpoint(req.getEndpoint())) {
            throw new IllegalArgumentException("Invalid or untrusted push notification endpoint");
        }

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
     * Delete a single user notification by ID.
     */
    @Transactional
    public void deleteNotification(Long id, Long userId) {
        inAppNotificationRepository.deleteByIdAndUser_Id(id, userId);
    }

    /**
     * Clear all user notifications.
     */
    @Transactional
    public void clearAllNotifications(Long userId) {
        inAppNotificationRepository.deleteByUser_Id(userId);
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
        if (pushService == null) {
            return;
        }

        List<PushSubscription> subs = pushSubscriptionRepository.findByUser_Id(userId);
        if (subs.isEmpty()) return;

        String payload = String.format("{\"title\":\"%s\",\"body\":\"%s\",\"url\":\"%s\"}",
            escapeJson(title), escapeJson(message), linkUrl != null ? escapeJson(linkUrl) : "/");

        for (PushSubscription sub : subs) {
            if (!isValidPushEndpoint(sub.getEndpoint())) {
                log.warn("[WebPush] Skipping untrusted push endpoint for user #{}: {}", userId, sub.getEndpoint());
                continue;
            }

            CompletableFuture.runAsync(() -> {
                try {
                    Notification notification = new Notification(
                        sub.getEndpoint(),
                        sub.getP256dh(),
                        sub.getAuth(),
                        payload
                    );

                    // Send using RFC 8291 standard AES128GCM encryption required by modern Google FCM and Mozilla WebPush
                    HttpResponse response = pushService.send(notification, Encoding.AES128GCM);
                    int statusCode = response.getStatusLine().getStatusCode();
                    String responseBody = "";
                    if (response.getEntity() != null) {
                        try {
                            responseBody = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                        } catch (Exception ignored) {}
                    }

                    if (statusCode == 200 || statusCode == 201) {
                        log.info("[WebPush] Successfully delivered push to user #{}, HTTP {}", userId, statusCode);
                    } else if (statusCode == 404 || statusCode == 410) {
                        log.warn("[WebPush] Push endpoint expired (HTTP {}), removing endpoint for user #{}: {}", statusCode, userId, sub.getEndpoint());
                        pushSubscriptionRepository.deleteByEndpoint(sub.getEndpoint());
                    } else {
                        log.warn("[WebPush] Push service rejected delivery for user #{} with HTTP {}: {} | Endpoint: {}",
                            userId, statusCode, responseBody, sub.getEndpoint());
                    }
                } catch (Exception ex) {
                    log.error("[WebPush] Delivery exception for user #{}: {}", userId, ex.getMessage(), ex);
                }
            });
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
