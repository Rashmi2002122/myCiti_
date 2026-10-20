package com.myciti.service;

import com.myciti.dto.*;
import com.myciti.exception.ResourceNotFoundException;
import com.myciti.model.*;
import com.myciti.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdCampaignService {

    private final AdCampaignRepository adCampaignRepository;
    private final AdReportRepository adReportRepository;
    private final ListingRepository listingRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final ZoneId zoneId;

    @Autowired
    public AdCampaignService(
        AdCampaignRepository adCampaignRepository,
        AdReportRepository adReportRepository,
        ListingRepository listingRepository,
        SubmissionRepository submissionRepository,
        UserRepository userRepository,
        @Value("${app.timezone:Asia/Kolkata}") String timeZone
    ) {
        this.adCampaignRepository = adCampaignRepository;
        this.adReportRepository = adReportRepository;
        this.listingRepository = listingRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.zoneId = ZoneId.of(timeZone != null && !timeZone.isBlank() ? timeZone.trim() : "Asia/Kolkata");
    }

    public AdCampaignService(
        AdCampaignRepository adCampaignRepository,
        AdReportRepository adReportRepository,
        ListingRepository listingRepository,
        SubmissionRepository submissionRepository,
        UserRepository userRepository
    ) {
        this(adCampaignRepository, adReportRepository, listingRepository, submissionRepository, userRepository, "Asia/Kolkata");
    }

    /**
     * Create a new ad campaign (CLIENT or ADMIN).
     * Clients can only create campaigns for their own approved listings.
     */
    @CacheEvict(value = "activeAds", allEntries = true)
    @Transactional
    public AdCampaignResponse createCampaign(Long userId, AdCampaignRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (user.getRole() != Role.CLIENT && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only CLIENT and ADMIN accounts can create ad campaigns");
        }

        Listing listing = listingRepository.findById(req.getListingId())
            .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + req.getListingId()));

        if (user.getRole() == Role.CLIENT) {
            Submission sub = submissionRepository.findByListingId(listing.getId())
                .orElseThrow(() -> new AccessDeniedException("You do not own this listing"));
            if (!sub.getUser().getId().equals(userId)) {
                throw new AccessDeniedException("You can only promote your own approved listings");
            }
            if (req.getTargetType() == TargetType.BROADCAST) {
                throw new AccessDeniedException("Whole-city broadcast campaigns are reserved for administrators");
            }
        }

        if (req.getStartDate().isAfter(req.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        AdCampaign campaign = new AdCampaign();
        campaign.setListing(listing);
        campaign.setClientUser(user);

        // Robust title fallback: title -> headline -> listing title
        String rawTitle = req.getTitle();
        if (rawTitle == null || rawTitle.isBlank()) {
            rawTitle = (req.getHeadline() != null && !req.getHeadline().isBlank())
                ? req.getHeadline()
                : (listing.getTitle() != null ? listing.getTitle() + " Promotion" : "Ad Campaign");
        }
        String cleanTitle = rawTitle.trim();
        campaign.setTitle(cleanTitle.length() > 150 ? cleanTitle.substring(0, 150) : cleanTitle);

        campaign.setHeadline(req.getHeadline() != null ? req.getHeadline().trim() : null);
        campaign.setTargetType(req.getTargetType());

        // Default coordinates to listing location if not explicitly provided
        Double targetLat = req.getTargetLat() != null ? req.getTargetLat() : listing.getLatitude();
        Double targetLng = req.getTargetLng() != null ? req.getTargetLng() : listing.getLongitude();
        campaign.setTargetLat(targetLat);
        campaign.setTargetLng(targetLng);

        double radius = req.getRadiusKm() != null ? req.getRadiusKm() : 5.0;
        campaign.setRadiusKm(Math.max(0.5, Math.min(25.0, radius)));

        String resolvedCity = req.getTargetCity();
        if (resolvedCity == null || resolvedCity.isBlank()) {
            resolvedCity = listing.getCity();
        }
        campaign.setTargetCity(resolvedCity != null && !resolvedCity.isBlank() ? resolvedCity.trim() : "Kolkata");

        campaign.setStartDate(req.getStartDate());
        campaign.setEndDate(req.getEndDate());
        campaign.setStatus(user.getRole() == Role.ADMIN ? CampaignStatus.APPROVED : CampaignStatus.PENDING);

        AdCampaign saved = adCampaignRepository.save(campaign);
        return AdCampaignResponse.fromEntity(saved);
    }

    /**
     * Get campaigns for client's dashboard.
     */
    @Transactional(readOnly = true)
    public List<AdCampaignResponse> getClientCampaigns(Long clientId) {
        return adCampaignRepository.findByClientUser_IdOrderByCreatedAtDesc(clientId).stream()
            .map(AdCampaignResponse::fromEntity)
            .toList();
    }

    /**
     * Pause campaign.
     */
    @CacheEvict(value = "activeAds", allEntries = true)
    @Transactional
    public AdCampaignResponse pauseCampaign(Long userId, Long campaignId) {
        AdCampaign campaign = getCampaignOrThrow(campaignId);
        checkManagePermission(userId, campaign);

        campaign.setStatus(CampaignStatus.PAUSED);
        campaign.setUpdatedAt(Instant.now());
        return AdCampaignResponse.fromEntity(adCampaignRepository.save(campaign));
    }

    /**
     * Resume campaign.
     */
    @CacheEvict(value = "activeAds", allEntries = true)
    @Transactional
    public AdCampaignResponse resumeCampaign(Long userId, Long campaignId) {
        AdCampaign campaign = getCampaignOrThrow(campaignId);
        checkManagePermission(userId, campaign);

        campaign.setStatus(CampaignStatus.APPROVED);
        campaign.setUpdatedAt(Instant.now());
        return AdCampaignResponse.fromEntity(adCampaignRepository.save(campaign));
    }

    /**
     * Active ads matching user's coarse location or city (Public & Users).
     * Caches non-coordinate queries (e.g. city landing view) in memory.
     */
    @Cacheable(value = "activeAds", key = "{#userLat, #userLng, #city}", condition = "#userLat == null && #userLng == null")
    @Transactional(readOnly = true)
    public List<ActiveAdDto> getActiveAds(Double userLat, Double userLng, String city) {
        LocalDate today = LocalDate.now(zoneId);
        List<AdCampaign> active = adCampaignRepository.findActiveApprovedCampaigns(today);
        List<ActiveAdDto> results = new ArrayList<>();

        for (AdCampaign c : active) {
            Listing l = c.getListing();
            if (l == null || l.getStatus() != Status.PUBLISHED) {
                continue;
            }

            Double distanceKm = null;
            boolean matches = false;

            if (c.getTargetType() == TargetType.BROADCAST) {
                matches = true;
                if (userLat != null && userLng != null && l.getLatitude() != 0.0) {
                    distanceKm = GeoTargetingService.haversineKm(userLat, userLng, l.getLatitude(), l.getLongitude());
                }
            } else if (c.getTargetType() == TargetType.CITY) {
                if (city != null && c.getTargetCity() != null && city.equalsIgnoreCase(c.getTargetCity().trim())) {
                    matches = true;
                } else if (city == null) {
                    matches = true; // anonymous / general view
                }
                if (userLat != null && userLng != null && l.getLatitude() != 0.0) {
                    distanceKm = GeoTargetingService.haversineKm(userLat, userLng, l.getLatitude(), l.getLongitude());
                }
            } else {
                // RADIUS
                if (userLat != null && userLng != null && c.getTargetLat() != null && c.getTargetLng() != null) {
                    distanceKm = GeoTargetingService.haversineKm(userLat, userLng, c.getTargetLat(), c.getTargetLng());
                    if (distanceKm <= c.getRadiusKm()) {
                        matches = true;
                    }
                } else if (userLat == null && userLng == null && city != null && l.getCity() != null && city.equalsIgnoreCase(l.getCity())) {
                    // Fallback for visitors with only city selected
                    matches = true;
                }
            }

            if (matches) {
                String primaryImageUrl = (l.getImages() != null && !l.getImages().isEmpty())
                    ? l.getImages().get(0).getUrl()
                    : null;

                results.add(new ActiveAdDto(
                    c.getId(),
                    l.getId(),
                    c.getTitle(),
                    c.getHeadline(),
                    l.getCategory() != null ? l.getCategory().name() : null,
                    l.getAddress(),
                    l.getLandmark(),
                    l.getCity(),
                    l.getLatitude(),
                    l.getLongitude(),
                    l.getOfferText(),
                    l.getContactPhone(),
                    primaryImageUrl,
                    distanceKm,
                    true
                ));
            }
        }

        return results;
    }

    /**
     * Record impression count.
     */
    @Transactional
    public void recordImpressions(Long campaignId, long count) {
        if (count > 0 && count <= 50) {
            adCampaignRepository.incrementImpressions(campaignId, count);
        }
    }

    /**
     * Record click count.
     */
    @Transactional
    public void recordClick(Long campaignId) {
        adCampaignRepository.incrementClicks(campaignId);
    }

    /**
     * Report an ad campaign.
     */
    @Transactional
    public void reportAd(Long reporterUserId, Long campaignId, AdReportRequest req) {
        AdCampaign campaign = getCampaignOrThrow(campaignId);
        User reporter = (reporterUserId != null) ? userRepository.findById(reporterUserId).orElse(null) : null;

        AdReport report = new AdReport(campaign, reporter, req.getReason().trim(), req.getDetails());
        adReportRepository.save(report);
    }

    // --- Admin Operations ---

    @Transactional(readOnly = true)
    public List<AdCampaignResponse> getAllCampaignsAdmin(CampaignStatus status) {
        if (status != null) {
            return adCampaignRepository.findByStatusOrderByCreatedAtDesc(status).stream()
                .map(AdCampaignResponse::fromEntity)
                .toList();
        }
        return adCampaignRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(AdCampaignResponse::fromEntity)
            .toList();
    }

    @CacheEvict(value = "activeAds", allEntries = true)
    @Transactional
    public AdCampaignResponse approveCampaignAdmin(Long campaignId) {
        AdCampaign campaign = getCampaignOrThrow(campaignId);
        campaign.setStatus(CampaignStatus.APPROVED);
        campaign.setRejectionReason(null);
        campaign.setUpdatedAt(Instant.now());
        return AdCampaignResponse.fromEntity(adCampaignRepository.save(campaign));
    }

    @CacheEvict(value = "activeAds", allEntries = true)
    @Transactional
    public AdCampaignResponse rejectCampaignAdmin(Long campaignId, String reason) {
        AdCampaign campaign = getCampaignOrThrow(campaignId);
        campaign.setStatus(CampaignStatus.REJECTED);
        campaign.setRejectionReason(reason);
        campaign.setUpdatedAt(Instant.now());
        return AdCampaignResponse.fromEntity(adCampaignRepository.save(campaign));
    }

    @CacheEvict(value = "activeAds", allEntries = true)
    @Transactional
    public void deleteCampaign(Long userId, Long campaignId) {
        AdCampaign campaign = getCampaignOrThrow(campaignId);
        checkManagePermission(userId, campaign);
        adCampaignRepository.delete(campaign);
    }

    @CacheEvict(value = "activeAds", allEntries = true)
    @Transactional
    public void deleteCampaignAdmin(Long campaignId) {
        AdCampaign campaign = getCampaignOrThrow(campaignId);
        adCampaignRepository.delete(campaign);
    }

    @Transactional(readOnly = true)
    public List<AdReportDto> getAllReportsAdmin() {
        return adReportRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(AdReportDto::fromEntity)
            .toList();
    }

    private AdCampaign getCampaignOrThrow(Long id) {
        return adCampaignRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + id));
    }

    private void checkManagePermission(Long userId, AdCampaign campaign) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (user.getRole() != Role.ADMIN && !campaign.getClientUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to modify this campaign");
        }
    }
}
