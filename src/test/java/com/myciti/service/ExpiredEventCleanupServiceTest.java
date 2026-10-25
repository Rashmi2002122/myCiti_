package com.myciti.service;

import com.myciti.model.Category;
import com.myciti.model.Listing;
import com.myciti.model.ListingImage;
import com.myciti.model.Submission;
import com.myciti.model.SubmissionImage;
import com.myciti.repository.InAppNotificationRepository;
import com.myciti.repository.ListingRepository;
import com.myciti.repository.RefreshTokenRepository;
import com.myciti.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExpiredEventCleanupServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private InAppNotificationRepository inAppNotificationRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private ExpiredEventCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        cleanupService = new ExpiredEventCleanupService(
                listingRepository,
                submissionRepository,
                storageService,
                inAppNotificationRepository,
                refreshTokenRepository,
                "Asia/Kolkata"
        );
    }

    @Test
    void testCleanupExpiredEvents_DeletesListingsAndPhotos() {
        Listing expiredListing = new Listing();
        expiredListing.setId(10L);
        expiredListing.setTitle("Old Fest");
        expiredListing.setCategory(Category.MELA);
        expiredListing.setEndDate(LocalDate.now().minusDays(2));
        expiredListing.addImage(new ListingImage(expiredListing, "/uploads/old-fest-photo.jpg", 1));

        Submission expiredSubmission = new Submission();
        expiredSubmission.setId(20L);
        expiredSubmission.setTitle("Old Submission");
        expiredSubmission.setEndDate(LocalDate.now().minusDays(5));
        expiredSubmission.addImage(new SubmissionImage(expiredSubmission, "/uploads/old-sub-photo.jpg", 1));

        when(listingRepository.findByEndDateBefore(any(LocalDate.class)))
                .thenReturn(List.of(expiredListing));
        when(submissionRepository.findByEndDateBefore(any(LocalDate.class)))
                .thenReturn(List.of(expiredSubmission));

        cleanupService.cleanupExpiredEventsAndStorage();

        verify(listingRepository, times(1)).delete(expiredListing);
        verify(submissionRepository, times(1)).delete(expiredSubmission);
        verify(storageService, times(1)).deleteImage("/uploads/old-fest-photo.jpg");
        verify(storageService, times(1)).deleteImage("/uploads/old-sub-photo.jpg");
    }

    @Test
    void testCleanupExpiredEvents_NoExpiredEvents_DoesNothing() {
        when(listingRepository.findByEndDateBefore(any(LocalDate.class)))
                .thenReturn(List.of());
        when(submissionRepository.findByEndDateBefore(any(LocalDate.class)))
                .thenReturn(List.of());

        cleanupService.cleanupExpiredEventsAndStorage();

        verify(listingRepository, never()).delete(any());
        verify(submissionRepository, never()).delete(any());
        verify(storageService, never()).deleteImage(any());
    }

    @Test
    void testCleanupOldNotificationsAndTokens_InvokesPruneQueries() {
        when(inAppNotificationRepository.deleteByCreatedAtBefore(any(Instant.class))).thenReturn(5);
        when(refreshTokenRepository.deleteAllRevokedAndExpired(any(Instant.class))).thenReturn(3);

        cleanupService.cleanupOldNotificationsAndTokens();

        verify(inAppNotificationRepository, times(1)).deleteByCreatedAtBefore(any(Instant.class));
        verify(refreshTokenRepository, times(1)).deleteAllRevokedAndExpired(any(Instant.class));
    }
}
