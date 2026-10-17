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
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final InAppNotificationRepository inAppNotificationRepository;
    private final UserRepository userRepository;
    private final HttpClient httpClient;

    @Value("${app.webpush.public-key:}")
    private String vapidPublicKey;

    @Value("${app.webpush.private-key:}")
    private String vapidPrivateKey;

    @Value("${app.webpush.subject:mailto:admin@myciti.com}")
    private String vapidSubject;

    private PrivateKey vapidPrivateKeyObj;

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

    @PostConstruct
    public void initVapidKey() {
        if (vapidPrivateKey != null && !vapidPrivateKey.isBlank()) {
            try {
                byte[] raw = Base64.getUrlDecoder().decode(vapidPrivateKey);
                BigInteger s = new BigInteger(1, raw);
                AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
                params.init(new ECGenParameterSpec("secp256r1"));
                ECParameterSpec ecSpec = params.getParameterSpec(ECParameterSpec.class);
                ECPrivateKeySpec keySpec = new ECPrivateKeySpec(s, ecSpec);
                KeyFactory kf = KeyFactory.getInstance("EC");
                this.vapidPrivateKeyObj = kf.generatePrivate(keySpec);
                log.info("[WebPush] VAPID ES256 key initialized successfully for background push delivery.");
            } catch (Exception e) {
                log.warn("[WebPush] Could not parse VAPID private key: {}", e.getMessage());
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

        for (PushSubscription sub : subs) {
            CompletableFuture.runAsync(() -> {
                try {
                    String token = generateVapidToken(sub.getEndpoint());
                    HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(sub.getEndpoint()))
                        .header("TTL", "86400")
                        .header("Urgency", "high")
                        .timeout(Duration.ofSeconds(6));

                    if (token != null && vapidPublicKey != null && !vapidPublicKey.isBlank()) {
                        reqBuilder.header("Authorization", "vapid t=" + token + ", k=" + vapidPublicKey);
                    }

                    reqBuilder.POST(HttpRequest.BodyPublishers.noBody());

                    httpClient.sendAsync(reqBuilder.build(), HttpResponse.BodyHandlers.discarding())
                        .thenAccept(res -> {
                            if (res.statusCode() == 404 || res.statusCode() == 410) {
                                log.info("[WebPush] Pruning expired endpoint: {}", sub.getEndpoint());
                                pushSubscriptionRepository.deleteByEndpoint(sub.getEndpoint());
                            } else {
                                log.debug("[WebPush] Push sent to user #{}, status: {}", userId, res.statusCode());
                            }
                        })
                        .exceptionally(ex -> {
                            log.debug("[WebPush] Delivery attempt completed for user #{}", userId);
                            return null;
                        });
                } catch (Exception e) {
                    log.warn("[WebPush] Failed push delivery to user #{}: {}", userId, e.getMessage());
                }
            });
        }
    }

    private String generateVapidToken(String endpoint) {
        if (vapidPrivateKeyObj == null || endpoint == null) return null;
        try {
            URI uri = URI.create(endpoint);
            String origin = uri.getScheme() + "://" + uri.getAuthority();
            Date now = new Date();
            Date exp = new Date(now.getTime() + 12 * 3600 * 1000); // 12 hours

            return Jwts.builder()
                .header().add("typ", "JWT").add("alg", "ES256").and()
                .audience().add(origin).and()
                .expiration(exp)
                .subject(vapidSubject)
                .signWith(vapidPrivateKeyObj, Jwts.SIG.ES256)
                .compact();
        } catch (Exception e) {
            log.warn("[WebPush] Failed to create VAPID token: {}", e.getMessage());
            return null;
        }
    }
}
