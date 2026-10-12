package com.myciti.service;

import com.myciti.model.Listing;
import com.myciti.model.ListingImage;
import com.myciti.model.Submission;
import com.myciti.model.SubmissionImage;
import com.myciti.repository.ListingRepository;
import com.myciti.repository.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ZoneId kolkataZone;

    public ExpiredEventCleanupService(ListingRepository listingRepository,
                                      SubmissionRepository submissionRepository,
                                      StorageService storageService,
                                      @Value("${app.timezone:Asia/Kolkata}") String timeZone) {
        this.listingRepository = listingRepository;
        this.submissionRepository = submissionRepository;
        this.storageService = storageService;
        this.kolkataZone = ZoneId.of(timeZone);
    }

    /**
     * Runs every day at 02:00 AM Kolkata time to delete finished events/ads
     * and their associated photos from disk to free up database and storage space.
     * Also runs on application startup after a 30-second initial delay.
     */
    @Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Kolkata")
    @Transactional
    public void cleanupExpiredEventsAndStorage() {
        LocalDate todayInKolkata = ZonedDateTime.now(kolkataZone).toLocalDate();
        log.info("[Cleanup] Starting automated cleanup for expired events (endDate < {})...", todayInKolkata);

        int deletedListingsCount = 0;
        int deletedSubmissionsCount = 0;
        int deletedPhotosCount = 0;
        Set<String> imageUrlsToDelete = new HashSet<>();

        try {
            // 1. Identify and delete expired live listings
            List<Listing> expiredListings = listingRepository.findByEndDateBefore(todayInKolkata);
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

            // 2. Identify and delete expired user submissions
            List<Submission> expiredSubmissions = submissionRepository.findByEndDateBefore(todayInKolkata);
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
}
