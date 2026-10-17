package com.myciti.service;

import com.myciti.dto.AdminSubmissionsResponse;
import com.myciti.dto.SubmissionImageDto;
import com.myciti.dto.SubmissionRequest;
import com.myciti.dto.SubmissionResponse;
import com.myciti.exception.ResourceNotFoundException;
import com.myciti.model.*;
import com.myciti.repository.AuditLogRepository;
import com.myciti.repository.ListingRepository;
import com.myciti.repository.SubmissionRepository;
import com.myciti.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");

    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final AuditLogRepository auditLogRepository;
    private final StorageService storageService;
    private final EmailNotificationService emailNotificationService;

    @Value("${app.city.bbox.min-lat:6.00}")
    private double minLat;

    @Value("${app.city.bbox.max-lat:37.50}")
    private double maxLat;

    @Value("${app.city.bbox.min-lng:68.00}")
    private double minLng;

    @Value("${app.city.bbox.max-lng:97.50}")
    private double maxLng;

    public SubmissionService(SubmissionRepository submissionRepository,
                             UserRepository userRepository,
                             ListingRepository listingRepository,
                             AuditLogRepository auditLogRepository,
                             StorageService storageService,
                             EmailNotificationService emailNotificationService) {
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.auditLogRepository = auditLogRepository;
        this.storageService = storageService;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional
    public SubmissionResponse createSubmission(Long userId, SubmissionRequest req) {
        User user = getUserOrThrow(userId);

        if (user.getRole() != Role.CLIENT && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only CLIENT and ADMIN accounts can post advertisements and listings.");
        }

        // Rule 8: Profile must be complete before submitting
        if (!user.isProfileComplete()) {
            throw new IllegalArgumentException("Please complete your profile before posting an advertisement");
        }

        // Rate limits: max 5 submissions per 24 hours
        Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        long submissionsPastDay = submissionRepository.countSubmissionsSince(userId, twentyFourHoursAgo);
        if (submissionsPastDay >= 5) {
            throw new IllegalArgumentException("Daily limit reached: maximum 5 advertisements per day");
        }

        // Rate limits: max 3 PENDING at a time per user
        long pendingCount = submissionRepository.countByUserIdAndStatus(userId, SubmissionStatus.PENDING);
        if (pendingCount >= 3) {
            throw new IllegalArgumentException("Pending limit reached: maximum 3 advertisements pending review at a time");
        }

        // Validate and sanitize
        validateSubmissionDetails(req);
        String contactPhone = resolveAndValidatePhone(req.getContactPhone(), user.getPhone());

        Submission submission = new Submission();
        submission.setUser(user);
        submission.setStatus(SubmissionStatus.PENDING);
        applySanitizedRequest(submission, req, contactPhone);

        Submission saved = submissionRepository.save(submission);

        // Notify owner/admin via email
        emailNotificationService.notifyOwnerNewSubmission(
                saved.getTitle(),
                user.getName(),
                user.getEmail(),
                saved.getId()
        );

        return SubmissionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getUserSubmissions(Long userId, SubmissionStatus status) {
        List<Submission> list;
        if (status != null) {
            list = submissionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        } else {
            list = submissionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return list.stream().map(SubmissionResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getSubmissionById(Long id, Long userId) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with ID: " + id));

        // Ownership enforcement
        if (!submission.getUser().getId().equals(userId)) {
            User user = getUserOrThrow(userId);
            if (user.getRole() != Role.ADMIN) {
                throw new AccessDeniedException("You do not have permission to access this submission");
            }
        }
        return SubmissionResponse.fromEntity(submission);
    }

    @Transactional
    public SubmissionResponse updateSubmission(Long id, Long userId, SubmissionRequest req) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with ID: " + id));

        if (!submission.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to edit this submission");
        }

        User user = submission.getUser();
        if (!user.isProfileComplete()) {
            throw new IllegalArgumentException("Please complete your profile before editing an advertisement");
        }

        validateSubmissionDetails(req);
        String contactPhone = resolveAndValidatePhone(req.getContactPhone(), user.getPhone());

        // Workflow Rule 5: Editing an APPROVED submission creates a new PENDING revision
        if (submission.getStatus() == SubmissionStatus.APPROVED) {
            long pendingCount = submissionRepository.countByUserIdAndStatus(userId, SubmissionStatus.PENDING);
            if (pendingCount >= 3) {
                throw new IllegalArgumentException("Pending limit reached: maximum 3 advertisements pending review at a time");
            }

            Submission revision = new Submission();
            revision.setUser(user);
            revision.setStatus(SubmissionStatus.PENDING);
            revision.setListingId(submission.getListingId()); // Point to existing live listing for in-place update on approval
            applySanitizedRequest(revision, req, contactPhone);

            // Clone existing photos so the user doesn't lose photos on revision
            if (submission.getImages() != null) {
                for (SubmissionImage origImg : submission.getImages()) {
                    revision.addImage(new SubmissionImage(revision, origImg.getUrl(), origImg.getSortOrder()));
                }
            }

            Submission savedRevision = submissionRepository.save(revision);
            return SubmissionResponse.fromEntity(savedRevision);
        }

        // Workflow Rule 4: User can edit a PENDING or DECLINED submission; saving DECLINED resets to PENDING & clears reason
        applySanitizedRequest(submission, req, contactPhone);
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setDeclineReason(null);
        submission.setReviewedBy(null);
        submission.setReviewedAt(null);

        Submission updated = submissionRepository.save(submission);
        return SubmissionResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteSubmission(Long id, Long userId) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with ID: " + id));

        if (!submission.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to delete this submission");
        }

        // Workflow Rule 6: User can delete PENDING or DECLINED submission. For APPROVED, offer Unpublish.
        if (submission.getStatus() == SubmissionStatus.APPROVED) {
            throw new IllegalArgumentException("Approved advertisements cannot be deleted directly. Use Unpublish instead.");
        }

        submissionRepository.delete(submission);
    }

    @Transactional
    public SubmissionResponse unpublishSubmission(Long id, Long userId) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with ID: " + id));

        if (!submission.getUser().getId().equals(userId)) {
            User user = getUserOrThrow(userId);
            if (user.getRole() != Role.ADMIN) {
                throw new AccessDeniedException("You do not have permission to unpublish this submission");
            }
        }

        if (submission.getStatus() != SubmissionStatus.APPROVED || submission.getListingId() == null) {
            throw new IllegalArgumentException("Only approved live advertisements can be unpublished");
        }

        // Hide listing by setting status to DRAFT
        Listing listing = listingRepository.findById(submission.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Associated listing not found with ID: " + submission.getListingId()));

        listing.setStatus(Status.DRAFT);
        listingRepository.save(listing);

        // Audit log entry
        auditLogRepository.save(new AuditLog(
                userId,
                "UNPUBLISH_SUBMISSION",
                "Unpublished listing ID " + listing.getId() + " from submission ID " + submission.getId()
        ));

        return SubmissionResponse.fromEntity(submission);
    }

    @Transactional
    public List<SubmissionImageDto> addImagesToSubmission(Long id, Long userId, List<MultipartFile> files) throws IOException {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with ID: " + id));

        if (!submission.getUser().getId().equals(userId)) {
            User user = getUserOrThrow(userId);
            if (user.getRole() != Role.ADMIN) {
                throw new AccessDeniedException("You do not have permission to upload photos to this submission");
            }
        }

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No image files provided");
        }

        int currentCount = submission.getImages().size();
        if (currentCount + files.size() > 5) {
            throw new IllegalArgumentException(String.format(
                    "Maximum 5 photos allowed per advertisement (currently has %d, attempting to add %d)",
                    currentCount, files.size()));
        }

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String url = storageService.storeImage(file);
            SubmissionImage img = new SubmissionImage(submission, url, currentCount + i);
            submission.addImage(img);
        }

        Submission saved = submissionRepository.save(submission);
        return saved.getImages().stream()
                .map(img -> new SubmissionImageDto(img.getId(), img.getUrl(), img.getSortOrder()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteImageFromSubmission(Long id, Long imageId, Long userId) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with ID: " + id));

        if (!submission.getUser().getId().equals(userId)) {
            User user = getUserOrThrow(userId);
            if (user.getRole() != Role.ADMIN) {
                throw new AccessDeniedException("You do not have permission to delete photos from this submission");
            }
        }

        SubmissionImage toRemove = submission.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Photo not found with ID: " + imageId));

        storageService.deleteImage(toRemove.getUrl());
        submission.removeImage(toRemove);

        // Re-index remaining images
        for (int i = 0; i < submission.getImages().size(); i++) {
            submission.getImages().get(i).setSortOrder(i);
        }

        submissionRepository.save(submission);
    }

    // --- Admin Operations ---

    @Transactional(readOnly = true)
    public AdminSubmissionsResponse getAdminSubmissions(SubmissionStatus status) {
        List<Submission> items;
        if (status != null) {
            // Oldest first for review queue
            items = submissionRepository.findByStatusOrderByCreatedAtAsc(status);
        } else {
            items = submissionRepository.findAllByOrderByCreatedAtAsc();
        }

        long pendingCount = submissionRepository.countByStatus(SubmissionStatus.PENDING);
        long approvedCount = submissionRepository.countByStatus(SubmissionStatus.APPROVED);
        long declinedCount = submissionRepository.countByStatus(SubmissionStatus.DECLINED);
        long totalCount = pendingCount + approvedCount + declinedCount;

        List<SubmissionResponse> responses = items.stream()
                .map(SubmissionResponse::fromEntity)
                .collect(Collectors.toList());

        AdminSubmissionsResponse.Counts counts = new AdminSubmissionsResponse.Counts(
                pendingCount, approvedCount, declinedCount, totalCount
        );

        return new AdminSubmissionsResponse(responses, counts);
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getAdminSubmissionById(Long id) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with ID: " + id));
        return SubmissionResponse.fromEntity(submission);
    }

    @Caching(evict = {
        @CacheEvict(value = "listings", allEntries = true),
        @CacheEvict(value = "listingById", allEntries = true)
    })
    @Transactional
    public SubmissionResponse approveSubmission(Long id, User adminUser) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with ID: " + id));

        // Idempotency: If already approved, return existing approval without creating duplicate listings
        if (submission.getStatus() == SubmissionStatus.APPROVED) {
            return SubmissionResponse.fromEntity(submission);
        }

        Listing listing;
        if (submission.getListingId() != null) {
            // Updating existing live listing from a revision (fallback to new listing if live listing was previously deleted)
            listing = listingRepository.findById(submission.getListingId())
                    .orElseGet(() -> {
                        log.warn("Referenced live listing #{} for submission #{} was not found (may have been deleted). Creating a new live listing.",
                                submission.getListingId(), id);
                        return new Listing();
                    });
        } else {
            // Creating a new live listing
            listing = new Listing();
        }

        // Copy fields from submission to listing
        listing.setTitle(submission.getTitle());
        listing.setCategory(submission.getCategory());
        listing.setDescription(submission.getDescription());
        listing.setAddress(submission.getAddress());
        listing.setLandmark(submission.getLandmark());
        listing.setCity(submission.getCity() != null && !submission.getCity().trim().isEmpty() ? submission.getCity().trim() : "Kolkata");
        listing.setLatitude(submission.getLatitude());
        listing.setLongitude(submission.getLongitude());
        listing.setStartDate(submission.getStartDate());
        listing.setEndDate(submission.getEndDate());
        listing.setDailyOpenTime(submission.getDailyOpenTime());
        listing.setDailyCloseTime(submission.getDailyCloseTime());
        listing.setOfferText(submission.getOfferText());
        listing.setContactPhone(submission.getContactPhone());
        listing.setStatus(Status.PUBLISHED);
        listing.setSourceSubmissionId(submission.getId());

        // Copy images
        listing.getImages().clear();
        if (submission.getImages() != null) {
            for (SubmissionImage subImg : submission.getImages()) {
                listing.addImage(new ListingImage(listing, subImg.getUrl(), subImg.getSortOrder()));
            }
        }

        Listing savedListing = listingRepository.save(listing);

        // Update submission status and pointers
        submission.setStatus(SubmissionStatus.APPROVED);
        submission.setListingId(savedListing.getId());
        submission.setDeclineReason(null);
        submission.setReviewedBy(adminUser != null ? adminUser.getEmail() : "ADMIN");
        submission.setReviewedAt(Instant.now());

        Submission savedSubmission = submissionRepository.save(submission);

        // Audit log entry
        auditLogRepository.save(new AuditLog(
                (adminUser != null && adminUser.getId() != null) ? adminUser.getId() : 0L,
                "APPROVE_SUBMISSION",
                "Approved submission ID " + id + ", published Listing ID " + savedListing.getId()
        ));

        // Notify user via email
        if (submission.getUser() != null) {
            emailNotificationService.notifyUserApproved(
                    submission.getUser().getEmail(),
                    submission.getUser().getName(),
                    submission.getTitle(),
                    savedListing.getId()
            );
        }

        return SubmissionResponse.fromEntity(savedSubmission);
    }

    @Transactional
    public SubmissionResponse declineSubmission(Long id, String reason, User adminUser) {
        if (reason == null || reason.trim().length() < 10) {
            throw new IllegalArgumentException("Decline reason is required (minimum 10 characters)");
        }

        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with ID: " + id));

        // Idempotency: If already declined with the same reason
        if (submission.getStatus() == SubmissionStatus.DECLINED && reason.trim().equals(submission.getDeclineReason())) {
            return SubmissionResponse.fromEntity(submission);
        }

        submission.setStatus(SubmissionStatus.DECLINED);
        submission.setDeclineReason(reason.trim());
        submission.setReviewedBy(adminUser != null ? adminUser.getEmail() : "ADMIN");
        submission.setReviewedAt(Instant.now());

        Submission saved = submissionRepository.save(submission);

        // Audit log entry
        auditLogRepository.save(new AuditLog(
                (adminUser != null && adminUser.getId() != null) ? adminUser.getId() : 0L,
                "DECLINE_SUBMISSION",
                "Declined submission ID " + id + ". Reason: " + reason.trim()
        ));

        // Notify user via email
        if (submission.getUser() != null) {
            emailNotificationService.notifyUserDeclined(
                    submission.getUser().getEmail(),
                    submission.getUser().getName(),
                    submission.getTitle(),
                    reason.trim()
            );
        }

        return SubmissionResponse.fromEntity(saved);
    }

    // --- Validation and Sanitization Helpers ---

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    private void validateSubmissionDetails(SubmissionRequest req) {
        // Location bounding box check
        Double lat = req.getLatitude();
        Double lng = req.getLongitude();
        if (lat == null || lng == null) {
            throw new IllegalArgumentException("Latitude and Longitude are required");
        }
        if (lat < minLat || lat > maxLat || lng < minLng || lng > maxLng) {
            throw new IllegalArgumentException(String.format(
                    "Location coordinates (%.4f, %.4f) are outside India region [%.2f-%.2f Lat, %.2f-%.2f Lng]",
                    lat, lng, minLat, maxLat, minLng, maxLng));
        }

        // Dates check: endDate >= startDate, not more than 60 days ahead
        LocalDate startDate = req.getStartDate();
        LocalDate endDate = req.getEndDate();
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and End date are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be earlier than start date");
        }

        LocalDate today = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata")).toLocalDate();
        if (startDate.isBefore(today.minusDays(1))) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
        if (startDate.isAfter(today.plusDays(60)) || endDate.isAfter(today.plusDays(60))) {
            throw new IllegalArgumentException("Advertisement dates cannot be scheduled more than 60 days in advance");
        }

        // Times check
        if (req.getDailyOpenTime() == null || req.getDailyCloseTime() == null) {
            throw new IllegalArgumentException("Daily open and close times are required");
        }
    }

    private String resolveAndValidatePhone(String contactPhone, String profilePhone) {
        String phoneToUse = (contactPhone != null && !contactPhone.trim().isEmpty())
                ? contactPhone.trim()
                : (profilePhone != null ? profilePhone.trim() : null);

        if (phoneToUse == null || phoneToUse.isEmpty()) {
            throw new IllegalArgumentException("A contact phone number is required");
        }

        String cleaned = phoneToUse.replaceAll("^(\\+91|\\+91-|\\+91\\s)", "").replaceAll("\\s+", "");
        if (!PHONE_PATTERN.matcher(cleaned).matches()) {
            throw new IllegalArgumentException("Contact phone must be a valid 10-digit Indian mobile number");
        }
        return cleaned;
    }

    private void applySanitizedRequest(Submission s, SubmissionRequest req, String contactPhone) {
        s.setCategory(req.getCategory());
        s.setTitle(sanitize(req.getTitle(), 255));
        s.setDescription(sanitize(req.getDescription(), 4000));
        s.setVenueName(sanitize(req.getVenueName(), 255));
        s.setAddress(sanitize(req.getAddress(), 500));
        s.setLandmark(sanitize(req.getLandmark(), 255));
        s.setCity(req.getCity() != null && !req.getCity().trim().isEmpty() ? sanitize(req.getCity(), 100) : "Kolkata");
        s.setLatitude(req.getLatitude());
        s.setLongitude(req.getLongitude());
        s.setStartDate(req.getStartDate());
        s.setEndDate(req.getEndDate());
        s.setDailyOpenTime(req.getDailyOpenTime());
        s.setDailyCloseTime(req.getDailyCloseTime());
        s.setOfferText(req.getOfferText() != null ? sanitize(req.getOfferText(), 500) : null);
        s.setContactPhone(contactPhone);
    }

    private String sanitize(String input, int maxLength) {
        if (input == null) return null;
        // Strip HTML tags and trim
        String stripped = input.replaceAll("<[^>]*>", "").trim();
        if (stripped.length() > maxLength) {
            return stripped.substring(0, maxLength);
        }
        return stripped;
    }
}
