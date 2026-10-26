package com.myciti.service;

import com.myciti.model.Listing;
import com.myciti.model.ListingImage;
import com.myciti.model.Submission;
import com.myciti.model.SubmissionImage;
import com.myciti.repository.InAppNotificationRepository;
import com.myciti.repository.ListingRepository;
import com.myciti.repository.RefreshTokenRepository;
import com.myciti.repository.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ExpiredEventCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ExpiredEventCleanupService.class);

    private final ListingRepository listingRepository;
    private final SubmissionRepository submissionRepository;
    private final StorageService storageService;
    private final InAppNotificationRepository inAppNotificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ZoneId kolkataZone;

    @Autowired
    public ExpiredEventCleanupService(ListingRepository listingRepository,
                                      SubmissionRepository submissionRepository,
                                      StorageService storageService,
                                      InAppNotificationRepository inAppNotificationRepository,
                                      RefreshTokenRepository refreshTokenRepository,
                                      @Value("${app.timezone:Asia/Kolkata}") String timeZone) {
        this.listingRepository = listingRepository;
        this.submissionRepository = submissionRepository;
        this.storageService = storageService;
        this.inAppNotificationRepository = inAppNotificationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.kolkataZone = ZoneId.of(timeZone);
    }

    public ExpiredEventCleanupService(ListingRepository listingRepository,
                                      SubmissionRepository submissionRepository,
                                      StorageService storageService,
                                      @Value("${app.timezone:Asia/Kolkata}") String timeZone) {
        this(listingRepository, submissionRepository, storageService, null, null, timeZone);
    }

    /**
     * Runs every day at 02:00 AM Kolkata time.
     * Enforces an archive retention policy: only deletes truly obsolete events/ads
     * that ended more than 180 days ago, allowing clients and users to retain their
     * post history and expired event badges in their profiles.
     */
    @Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Kolkata")
    @org.springframework.cache.annotation.Caching(evict = {
        @org.springframework.cache.annotation.CacheEvict(value = "listings", allEntries = true),
        @org.springframework.cache.annotation.CacheEvict(value = "listingById", allEntries = true)
    })
    @Transactional
    public void cleanupExpiredEventsAndStorage() {
        // Retain expired events for at least 180 days for client dashboard and public profile visibility
        LocalDate retentionCutoff = ZonedDateTime.now(kolkataZone).toLocalDate().minusDays(180);
        log.info("[Cleanup] Starting automated cleanup for archived events (endDate < {})...", retentionCutoff);

        int deletedListingsCount = 0;
        int deletedSubmissionsCount = 0;
        int deletedPhotosCount = 0;
        Set<String> imageUrlsToDelete = new HashSet<>();

        try {
            // 1. Identify and delete obsolete live listings past retention cutoff
            List<Listing> expiredListings = listingRepository.findByEndDateBefore(retentionCutoff);
            for (Listing listing : expiredListings) {
                if (listing.getImages() != null) {
                    for (ListingImage img : listing.getImages()) {
                        if (img.getUrl() != null) {
                            imageUrlsToDelete.add(img.getUrl());
                        }
                    }
                }
                listingRepository.delete(listing);
                deletedListingsCount++;
            }

            // 2. Identify and delete obsolete user submissions past retention cutoff
            List<Submission> expiredSubmissions = submissionRepository.findByEndDateBefore(retentionCutoff);
            for (Submission submission : expiredSubmissions) {
                if (submission.getImages() != null) {
                    for (SubmissionImage img : submission.getImages()) {
                        if (img.getUrl() != null) {
                            imageUrlsToDelete.add(img.getUrl());
                        }
                    }
                }
                submissionRepository.delete(submission);
                deletedSubmissionsCount++;
            }

            // 3. Delete image files from disk / storage
            for (String imageUrl : imageUrlsToDelete) {
                try {
                    storageService.deleteImage(imageUrl);
                    deletedPhotosCount++;
                } catch (Exception e) {
                    log.warn("[Cleanup] Failed to delete image file {}: {}", imageUrl, e.getMessage());
                }
            }

            if (deletedListingsCount > 0 || deletedSubmissionsCount > 0) {
                log.info("[Cleanup] Completed! Deleted {} expired listings, {} submissions, and pruned {} image files.",
                        deletedListingsCount, deletedSubmissionsCount, deletedPhotosCount);
            } else {
                log.info("[Cleanup] Completed! No expired events found.");
            }

        } catch (Exception e) {
            log.error("[Cleanup] Error during automated expired event cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Runs every day at 03:00 AM Kolkata time to purge old in-app notifications (> 30 days)
     * and expired/revoked refresh tokens to prevent unbounded table growth under high user activity.
     */
    @Scheduled(cron = "0 0 3 * * ?", zone = "Asia/Kolkata")
    @Transactional
    public void cleanupOldNotificationsAndTokens() {
        if (inAppNotificationRepository == null || refreshTokenRepository == null) {
            return;
        }
        try {
            Instant cutoff = Instant.now().minus(Duration.ofDays(30));
            int deletedNotifications = inAppNotificationRepository.deleteByCreatedAtBefore(cutoff);
            int deletedTokens = refreshTokenRepository.deleteAllRevokedAndExpired(Instant.now());

            if (deletedNotifications > 0 || deletedTokens > 0) {
                log.info("[Cleanup] Cleaned up {} old notifications (>30 days) and {} expired/revoked refresh tokens.",
                        deletedNotifications, deletedTokens);
            } else {
                log.info("[Cleanup] Completed table retention prune: no expired notifications or tokens found.");
            }
        } catch (Exception e) {
            log.error("[Cleanup] Error during automated data retention cleanup: {}", e.getMessage(), e);
        }
    }
}
