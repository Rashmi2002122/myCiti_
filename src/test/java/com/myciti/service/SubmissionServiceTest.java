package com.myciti.service;

import com.myciti.dto.SubmissionRequest;
import com.myciti.dto.SubmissionResponse;
import com.myciti.exception.ResourceNotFoundException;
import com.myciti.model.*;
import com.myciti.repository.AuditLogRepository;
import com.myciti.repository.ListingRepository;
import com.myciti.repository.SubmissionRepository;
import com.myciti.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private EmailNotificationService emailNotificationService;

    private SubmissionService submissionService;

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        submissionService = new SubmissionService(
                submissionRepository,
                userRepository,
                listingRepository,
                auditLogRepository,
                storageService,
                emailNotificationService
        );

        ReflectionTestUtils.setField(submissionService, "minLat", 6.00);
        ReflectionTestUtils.setField(submissionService, "maxLat", 37.50);
        ReflectionTestUtils.setField(submissionService, "minLng", 68.00);
        ReflectionTestUtils.setField(submissionService, "maxLng", 97.50);

        testUser = new User("sub-1", "user1@example.com", "User One", null);
        testUser.setId(1L);
        testUser.setPhone("9830112233");
        testUser.setProfileComplete(true);
        testUser.setRole(Role.CLIENT);

        otherUser = new User("sub-2", "user2@example.com", "User Two", null);
        otherUser.setId(2L);
        otherUser.setPhone("9830223344");
        otherUser.setProfileComplete(true);
        otherUser.setRole(Role.CLIENT);
    }

    private SubmissionRequest createValidRequest() {
        SubmissionRequest req = new SubmissionRequest();
        req.setCategory(Category.PANDAL);
        req.setTitle("Ballygunge Cultural Pandal");
        req.setDescription("Grand festive celebration");
        req.setVenueName("Ballygunge Park");
        req.setAddress("123 Ballygunge Circular Rd");
        req.setLandmark("Near Lake Mall");
        req.setLatitude(22.5200);
        req.setLongitude(88.3600);
        req.setStartDate(LocalDate.now().plusDays(2));
        req.setEndDate(LocalDate.now().plusDays(10));
        req.setDailyOpenTime(LocalTime.of(8, 0));
        req.setDailyCloseTime(LocalTime.of(23, 0));
        req.setContactPhone("9830112233");
        return req;
    }

    @Test
    void createSubmission_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(submissionRepository.countSubmissionsSince(eq(1L), any(Instant.class))).thenReturn(0L);
        when(submissionRepository.countByUserIdAndStatus(1L, SubmissionStatus.PENDING)).thenReturn(0L);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });

        SubmissionRequest req = createValidRequest();
        SubmissionResponse res = submissionService.createSubmission(1L, req);

        assertNotNull(res);
        assertEquals(100L, res.getId());
        assertEquals("Ballygunge Cultural Pandal", res.getTitle());
        assertEquals(SubmissionStatus.PENDING, res.getStatus());
        assertEquals("9830112233", res.getContactPhone());
    }

    @Test
    void createSubmission_consumerUserRole_throwsAccessDeniedException() {
        testUser.setRole(Role.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        SubmissionRequest req = createValidRequest();
        assertThrows(AccessDeniedException.class, () ->
                submissionService.createSubmission(1L, req));
    }

    @Test
    void createSubmission_profileIncomplete_throwsException() {
        testUser.setProfileComplete(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        SubmissionRequest req = createValidRequest();
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                submissionService.createSubmission(1L, req));
        assertTrue(ex.getMessage().contains("complete your profile"));
    }

    @Test
    void createSubmission_dailyLimitExceeded_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(submissionRepository.countSubmissionsSince(eq(1L), any(Instant.class))).thenReturn(5L);

        SubmissionRequest req = createValidRequest();
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                submissionService.createSubmission(1L, req));
        assertTrue(ex.getMessage().contains("Daily limit reached"));
    }

    @Test
    void createSubmission_pendingLimitExceeded_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(submissionRepository.countSubmissionsSince(eq(1L), any(Instant.class))).thenReturn(1L);
        when(submissionRepository.countByUserIdAndStatus(1L, SubmissionStatus.PENDING)).thenReturn(3L);

        SubmissionRequest req = createValidRequest();
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                submissionService.createSubmission(1L, req));
        assertTrue(ex.getMessage().contains("Pending limit reached"));
    }

    @Test
    void createSubmission_coordinatesOutsideBbox_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(submissionRepository.countSubmissionsSince(eq(1L), any(Instant.class))).thenReturn(0L);
        when(submissionRepository.countByUserIdAndStatus(1L, SubmissionStatus.PENDING)).thenReturn(0L);

        SubmissionRequest req = createValidRequest();
        req.setLatitude(40.7128); // New York coords, outside India bounds
        req.setLongitude(-74.0060);

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                submissionService.createSubmission(1L, req));
        assertTrue(ex.getMessage().contains("outside India region"));
    }

    @Test
    void createSubmission_endDateBeforeStartDate_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(submissionRepository.countSubmissionsSince(eq(1L), any(Instant.class))).thenReturn(0L);
        when(submissionRepository.countByUserIdAndStatus(1L, SubmissionStatus.PENDING)).thenReturn(0L);

        SubmissionRequest req = createValidRequest();
        req.setStartDate(LocalDate.now().plusDays(10));
        req.setEndDate(LocalDate.now().plusDays(2)); // before start date

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                submissionService.createSubmission(1L, req));
        assertTrue(ex.getMessage().contains("End date cannot be earlier than start date"));
    }

    @Test
    void getSubmissionById_userCannotReadOtherUserSubmission() {
        Submission sub = new Submission();
        sub.setId(10L);
        sub.setUser(otherUser);

        when(submissionRepository.findById(10L)).thenReturn(Optional.of(sub));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser)); // testUser is CLIENT, not ADMIN

        assertThrows(AccessDeniedException.class, () ->
                submissionService.getSubmissionById(10L, 1L));
    }

    @Test
    void updateSubmission_userCannotEditOtherUserSubmission() {
        Submission sub = new Submission();
        sub.setId(10L);
        sub.setUser(otherUser);

        when(submissionRepository.findById(10L)).thenReturn(Optional.of(sub));

        SubmissionRequest req = createValidRequest();
        assertThrows(AccessDeniedException.class, () ->
                submissionService.updateSubmission(10L, 1L, req));
    }

    @Test
    void deleteSubmission_userCannotDeleteOtherUserSubmission() {
        Submission sub = new Submission();
        sub.setId(10L);
        sub.setUser(otherUser);

        when(submissionRepository.findById(10L)).thenReturn(Optional.of(sub));

        assertThrows(AccessDeniedException.class, () ->
                submissionService.deleteSubmission(10L, 1L));
    }

    @Test
    void updateSubmission_declinedResetsToPendingAndClearsReason() {
        Submission sub = new Submission();
        sub.setId(10L);
        sub.setUser(testUser);
        sub.setStatus(SubmissionStatus.DECLINED);
        sub.setDeclineReason("Wrong location coordinates");
        sub.setReviewedBy("admin@myciti.com");
        sub.setReviewedAt(Instant.now());

        when(submissionRepository.findById(10L)).thenReturn(Optional.of(sub));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmissionRequest req = createValidRequest();
        req.setTitle("Updated Title");

        SubmissionResponse res = submissionService.updateSubmission(10L, 1L, req);

        assertEquals(SubmissionStatus.PENDING, res.getStatus());
        assertNull(res.getDeclineReason());
        assertNull(res.getReviewedBy());
        assertEquals("Updated Title", res.getTitle());
    }

    @Test
    void updateSubmission_approvedCreatesNewPendingRevision() {
        Submission sub = new Submission();
        sub.setId(10L);
        sub.setUser(testUser);
        sub.setStatus(SubmissionStatus.APPROVED);
        sub.setListingId(50L);
        sub.addImage(new SubmissionImage(sub, "/uploads/existing.jpg", 0));

        when(submissionRepository.findById(10L)).thenReturn(Optional.of(sub));
        when(submissionRepository.countByUserIdAndStatus(1L, SubmissionStatus.PENDING)).thenReturn(0L);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission revision = inv.getArgument(0);
            revision.setId(200L);
            return revision;
        });

        SubmissionRequest req = createValidRequest();
        req.setTitle("Revision Title");

        SubmissionResponse res = submissionService.updateSubmission(10L, 1L, req);

        assertEquals(200L, res.getId());
        assertEquals(SubmissionStatus.PENDING, res.getStatus());
        assertEquals(50L, res.getListingId()); // Points to existing listing
        assertEquals("Revision Title", res.getTitle());
        assertEquals(1, res.getImages().size());
        assertEquals("/uploads/existing.jpg", res.getImages().get(0).getUrl());
    }

    @Test
    void deleteSubmission_approvedCannotBeDeletedDirectly() {
        Submission sub = new Submission();
        sub.setId(10L);
        sub.setUser(testUser);
        sub.setStatus(SubmissionStatus.APPROVED);

        when(submissionRepository.findById(10L)).thenReturn(Optional.of(sub));

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                submissionService.deleteSubmission(10L, 1L));
        assertTrue(ex.getMessage().contains("Use Unpublish instead"));
    }

    @Test
    void unpublishSubmission_successSetsListingToDraft() {
        Submission sub = new Submission();
        sub.setId(10L);
        sub.setUser(testUser);
        sub.setStatus(SubmissionStatus.APPROVED);
        sub.setListingId(50L);

        Listing listing = new Listing();
        listing.setId(50L);
        listing.setStatus(Status.PUBLISHED);

        when(submissionRepository.findById(10L)).thenReturn(Optional.of(sub));
        when(listingRepository.findById(50L)).thenReturn(Optional.of(listing));

        SubmissionResponse res = submissionService.unpublishSubmission(10L, 1L);

        assertEquals(Status.DRAFT, listing.getStatus());
        verify(listingRepository).save(listing);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void addImagesToSubmission_success() throws Exception {
        Submission sub = new Submission();
        sub.setId(10L);
        sub.setUser(testUser);
        sub.setStatus(SubmissionStatus.PENDING);

        when(submissionRepository.findById(10L)).thenReturn(Optional.of(sub));
        when(storageService.storeImage(any())).thenReturn("/uploads/test-img.jpg");
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));

        org.springframework.web.multipart.MultipartFile mockFile = mock(org.springframework.web.multipart.MultipartFile.class);
        var images = submissionService.addImagesToSubmission(10L, 1L, java.util.List.of(mockFile));

        assertEquals(1, images.size());
        assertEquals("/uploads/test-img.jpg", images.get(0).getUrl());
        assertEquals(0, images.get(0).getSortOrder());
    }

    @Test
    void addImagesToSubmission_exceedsFiveImages_throwsException() {
        Submission sub = new Submission();
        sub.setId(10L);
        sub.setUser(testUser);
        sub.setStatus(SubmissionStatus.PENDING);

        // Already has 4 images
        for (int i = 0; i < 4; i++) {
            sub.addImage(new SubmissionImage(sub, "/uploads/img" + i + ".jpg", i));
        }

        when(submissionRepository.findById(10L)).thenReturn(Optional.of(sub));

        org.springframework.web.multipart.MultipartFile f1 = mock(org.springframework.web.multipart.MultipartFile.class);
        org.springframework.web.multipart.MultipartFile f2 = mock(org.springframework.web.multipart.MultipartFile.class);

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                submissionService.addImagesToSubmission(10L, 1L, java.util.List.of(f1, f2)));
        assertTrue(ex.getMessage().contains("Maximum 5 photos allowed"));
    }

    @Test
    void addImagesToSubmission_otherUserCannotUpload() {
        Submission sub = new Submission();
        sub.setId(10L);
        sub.setUser(otherUser);

        when(submissionRepository.findById(10L)).thenReturn(Optional.of(sub));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        org.springframework.web.multipart.MultipartFile mockFile = mock(org.springframework.web.multipart.MultipartFile.class);
        assertThrows(AccessDeniedException.class, () ->
                submissionService.addImagesToSubmission(10L, 1L, java.util.List.of(mockFile)));
    }

    @Test
    void declineSubmission_reasonTooShort_throwsException() {
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                submissionService.declineSubmission(10L, "short", testUser));
        assertTrue(ex.getMessage().contains("minimum 10 characters"));
    }

    @Test
    void declineSubmission_successSetsDeclinedAndAudits() {
        Submission sub = new Submission();
        sub.setId(10L);
        sub.setStatus(SubmissionStatus.PENDING);

        when(submissionRepository.findById(10L)).thenReturn(Optional.of(sub));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));

        User adminUser = new User("admin-sub", "admin@myciti.com", "Admin", null);
        adminUser.setId(99L);
        adminUser.setRole(Role.ADMIN);

        SubmissionResponse res = submissionService.declineSubmission(10L, "Wrong location coordinates", adminUser);

        assertEquals(SubmissionStatus.DECLINED, res.getStatus());
        assertEquals("Wrong location coordinates", res.getDeclineReason());
        assertEquals("admin@myciti.com", res.getReviewedBy());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void approveSubmission_createsNewListingAndAudits() {
        Submission sub = new Submission();
        sub.setId(10L);
        sub.setStatus(SubmissionStatus.PENDING);
        sub.setTitle("New Pandal");
        sub.setCategory(Category.PANDAL);
        sub.setDescription("Description");
        sub.setAddress("Address");
        sub.setLandmark("Landmark");
        sub.setLatitude(22.50);
        sub.setLongitude(88.35);
        sub.setStartDate(LocalDate.now());
        sub.setEndDate(LocalDate.now().plusDays(5));
        sub.setDailyOpenTime(LocalTime.of(8, 0));
        sub.setDailyCloseTime(LocalTime.of(22, 0));
        sub.setContactPhone("9830112233");

        when(submissionRepository.findById(10L)).thenReturn(Optional.of(sub));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> {
            Listing l = inv.getArgument(0);
            l.setId(555L);
            return l;
        });
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));

        User adminUser = new User("admin-sub", "admin@myciti.com", "Admin", null);
        adminUser.setId(99L);
        adminUser.setRole(Role.ADMIN);

        SubmissionResponse res = submissionService.approveSubmission(10L, adminUser);

        assertEquals(SubmissionStatus.APPROVED, res.getStatus());
        assertEquals(555L, res.getListingId());
        assertEquals("admin@myciti.com", res.getReviewedBy());
        verify(listingRepository).save(any(Listing.class));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void approveSubmission_idempotentDoesNotDuplicateListing() {
        Submission sub = new Submission();
        sub.setId(10L);
        sub.setStatus(SubmissionStatus.APPROVED);
        sub.setListingId(555L);

        when(submissionRepository.findById(10L)).thenReturn(Optional.of(sub));

        User adminUser = new User("admin-sub", "admin@myciti.com", "Admin", null);
        adminUser.setId(99L);

        SubmissionResponse res = submissionService.approveSubmission(10L, adminUser);

        assertEquals(SubmissionStatus.APPROVED, res.getStatus());
        assertEquals(555L, res.getListingId());
        // verify listingRepository.save was NOT called because already approved
        verify(listingRepository, never()).save(any(Listing.class));
    }

    @Test
    void approveSubmission_revisionUpdatesExistingListingInPlace() {
        Submission sub = new Submission();
        sub.setId(20L);
        sub.setStatus(SubmissionStatus.PENDING);
        sub.setListingId(555L);
        sub.setTitle("Updated Title Revision");
        sub.setCategory(Category.PANDAL);
        sub.setDescription("Updated description");
        sub.setAddress("Address");
        sub.setLandmark("Landmark");
        sub.setLatitude(22.50);
        sub.setLongitude(88.35);
        sub.setStartDate(LocalDate.now());
        sub.setEndDate(LocalDate.now().plusDays(5));
        sub.setDailyOpenTime(LocalTime.of(8, 0));
        sub.setDailyCloseTime(LocalTime.of(22, 0));
        sub.setContactPhone("9830112233");

        Listing existingListing = new Listing();
        existingListing.setId(555L);
        existingListing.setTitle("Old Title");

        when(submissionRepository.findById(20L)).thenReturn(Optional.of(sub));
        when(listingRepository.findById(555L)).thenReturn(Optional.of(existingListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));

        User adminUser = new User("admin-sub", "admin@myciti.com", "Admin", null);
        adminUser.setId(99L);

        SubmissionResponse res = submissionService.approveSubmission(20L, adminUser);

        assertEquals(SubmissionStatus.APPROVED, res.getStatus());
        assertEquals(555L, existingListing.getId());
        assertEquals("Updated Title Revision", existingListing.getTitle());
        verify(listingRepository).save(existingListing);
    }
}
