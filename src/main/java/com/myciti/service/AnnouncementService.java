package com.myciti.service;

import com.myciti.dto.AnnouncementRequest;
import com.myciti.dto.AnnouncementResponse;
import com.myciti.exception.ResourceNotFoundException;
import com.myciti.model.*;
import com.myciti.repository.AnnouncementRepository;
import com.myciti.repository.AuditLogRepository;
import com.myciti.repository.ListingRepository;
import com.myciti.repository.SubmissionRepository;
import com.myciti.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final ListingRepository listingRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final GeoTargetingService geoTargetingService;
    private final NotificationService notificationService;
    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AnnouncementService(
        AnnouncementRepository announcementRepository,
        ListingRepository listingRepository,
        SubmissionRepository submissionRepository,
        UserRepository userRepository,
        GeoTargetingService geoTargetingService,
        NotificationService notificationService,
        AuditLogRepository auditLogRepository
    ) {
        this.announcementRepository = announcementRepository;
        this.listingRepository = listingRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.geoTargetingService = geoTargetingService;
        this.notificationService = notificationService;
        this.auditLogRepository = auditLogRepository;
    }

    public AnnouncementService(
        AnnouncementRepository announcementRepository,
        ListingRepository listingRepository,
        SubmissionRepository submissionRepository,
        UserRepository userRepository,
        GeoTargetingService geoTargetingService,
        NotificationService notificationService
    ) {
        this(announcementRepository, listingRepository, submissionRepository, userRepository, geoTargetingService, notificationService, null);
    }

    /**
     * Create an announcement.
     * Admin broadcasts are dispatched immediately.
     * Client announcements require Admin approval.
     */
    @Transactional
    public AnnouncementResponse createAnnouncement(Long userId, AnnouncementRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (user.getRole() != Role.CLIENT && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only CLIENT and ADMIN accounts can send announcements");
        }

        Listing listing = null;
        if (req.getListingId() != null) {
            listing = listingRepository.findById(req.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + req.getListingId()));

            if (user.getRole() == Role.CLIENT) {
                Submission sub = submissionRepository.findFirstByListingIdOrderByCreatedAtDesc(listing.getId())
                    .orElseThrow(() -> new AccessDeniedException("You do not own this listing"));
                if (!sub.getUser().getId().equals(userId)) {
                    throw new AccessDeniedException("You can only announce for your own approved listings");
                }
            }
        } else if (user.getRole() == Role.CLIENT) {
            throw new IllegalArgumentException("Client announcements must be linked to an approved listing");
        }

        if (user.getRole() == Role.CLIENT && req.getTargetType() == TargetType.BROADCAST) {
            throw new AccessDeniedException("Whole-city broadcast announcements are reserved for administrators");
        }

        Announcement a = new Announcement();
        a.setAuthorUser(user);
        a.setListing(listing);
        a.setTitle(req.getTitle().trim());
        a.setMessage(req.getMessage().trim());
        a.setTargetType(req.getTargetType());

        Double targetLat = req.getTargetLat() != null ? req.getTargetLat() : (listing != null ? listing.getLatitude() : null);
        Double targetLng = req.getTargetLng() != null ? req.getTargetLng() : (listing != null ? listing.getLongitude() : null);
        a.setTargetLat(targetLat);
        a.setTargetLng(targetLng);

        double radius = req.getRadiusKm() != null ? req.getRadiusKm() : 5.0;
        a.setRadiusKm(Math.max(0.5, Math.min(25.0, radius)));
        a.setTargetCity(req.getTargetCity() != null && !req.getTargetCity().isBlank() ? req.getTargetCity().trim() : (listing != null ? listing.getCity() : null));

        if (user.getRole() == Role.ADMIN) {
            a.setStatus(AnnouncementStatus.SENT);
            a.setSentAt(Instant.now());
            Announcement saved = announcementRepository.save(a);
            dispatchAnnouncement(saved);
            return AnnouncementResponse.fromEntity(saved);
        } else {
            a.setStatus(AnnouncementStatus.PENDING);
            Announcement saved = announcementRepository.save(a);
            return AnnouncementResponse.fromEntity(saved);
        }
    }

    /**
     * Admin approves a client announcement and dispatches it.
     */
    @Transactional
    public AnnouncementResponse approveAnnouncementAdmin(Long announcementId) {
        Announcement a = announcementRepository.findById(announcementId)
            .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + announcementId));

        a.setStatus(AnnouncementStatus.SENT);
        a.setSentAt(Instant.now());
        a.setRejectionReason(null);
        Announcement saved = announcementRepository.save(a);

        if (auditLogRepository != null) {
            auditLogRepository.save(new AuditLog(
                resolveCurrentUserId(),
                "ANNOUNCEMENT_APPROVED",
                "Approved and dispatched announcement #" + announcementId + " ('" + saved.getTitle() + "')"
            ));
        }

        dispatchAnnouncement(saved);
        return AnnouncementResponse.fromEntity(saved);
    }

    /**
     * Admin rejects an announcement.
     */
    @Transactional
    public AnnouncementResponse rejectAnnouncementAdmin(Long announcementId, String reason) {
        Announcement a = announcementRepository.findById(announcementId)
            .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + announcementId));

        a.setStatus(AnnouncementStatus.REJECTED);
        a.setRejectionReason(reason);
        Announcement saved = announcementRepository.save(a);

        if (auditLogRepository != null) {
            auditLogRepository.save(new AuditLog(
                resolveCurrentUserId(),
                "ANNOUNCEMENT_REJECTED",
                "Rejected announcement #" + announcementId + ". Reason: " + reason
            ));
        }

        return AnnouncementResponse.fromEntity(saved);
    }

    /**
     * Admin permanently deletes an announcement.
     */
    @Transactional
    public void deleteAnnouncementAdmin(Long announcementId) {
        Announcement a = announcementRepository.findById(announcementId)
            .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + announcementId));
        announcementRepository.delete(a);

        if (auditLogRepository != null) {
            auditLogRepository.save(new AuditLog(
                resolveCurrentUserId(),
                "ANNOUNCEMENT_DELETED",
                "Admin permanently deleted announcement #" + announcementId
            ));
        }
    }

    private Long resolveCurrentUserId() {
        try {
            org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User user) {
                return user.getId();
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Get user's own announcements.
     */
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getMyAnnouncements(Long userId) {
        return announcementRepository.findByAuthorUser_IdOrderByCreatedAtDesc(userId).stream()
            .map(AnnouncementResponse::fromEntity)
            .toList();
    }

    /**
     * Admin: get all announcements.
     */
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getAllAnnouncementsAdmin() {
        return announcementRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(AnnouncementResponse::fromEntity)
            .toList();
    }

    private void dispatchAnnouncement(Announcement a) {
        List<User> targetRecipients = geoTargetingService.findTargetUsers(
            a.getTargetType(),
            a.getTargetLat(),
            a.getTargetLng(),
            a.getRadiusKm(),
            a.getTargetCity()
        );

        if (targetRecipients == null || targetRecipients.isEmpty()) {
            a.setRecipientCount(0);
            announcementRepository.save(a);
            return;
        }

        String linkUrl = a.getListing() != null ? "/?listingId=" + a.getListing().getId() : "/";
        int delivered = notificationService.dispatchNotificationToUsers(
            targetRecipients,
            a,
            a.getTitle(),
            a.getMessage(),
            linkUrl
        );

        a.setRecipientCount(delivered);
        announcementRepository.save(a);
    }
}
