package com.myciti.service;

import com.myciti.dto.ImageResponse;
import com.myciti.dto.ListingResponse;
import com.myciti.dto.PublicClientProfileResponse;
import com.myciti.exception.ResourceNotFoundException;
import com.myciti.model.*;
import com.myciti.repository.ClientProfileRepository;
import com.myciti.repository.ListingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClientProfileService {

    private final ClientProfileRepository clientProfileRepository;
    private final ListingRepository listingRepository;
    private final SlugService slugService;
    private final ZoneId zoneId;

    public ClientProfileService(
        ClientProfileRepository clientProfileRepository,
        ListingRepository listingRepository,
        SlugService slugService,
        @Value("${app.timezone:Asia/Kolkata}") String timezone
    ) {
        this.clientProfileRepository = clientProfileRepository;
        this.listingRepository = listingRepository;
        this.slugService = slugService;
        this.zoneId = ZoneId.of(timezone);
    }

    /**
     * Automatically creates a ClientProfile when a client request is approved or user becomes CLIENT.
     */
    @Transactional
    public ClientProfile createProfileForClient(User user, String businessName, ClientType clientType, String contactPhone) {
        if (clientProfileRepository.existsByUserId(user.getId())) {
            return clientProfileRepository.findByUserId(user.getId()).orElse(null);
        }

        String effectiveName = (businessName != null && !businessName.isBlank())
            ? businessName.trim()
            : (user.getName() != null && !user.getName().isBlank() ? user.getName().trim() : "Client " + user.getId());

        ClientType effectiveType = (clientType != null) ? clientType : ClientType.STORE;

        String slug = slugService.generateUniqueSlug(effectiveName);

        ClientProfile profile = new ClientProfile();
        profile.setUser(user);
        profile.setSlug(slug);
        profile.setBusinessName(effectiveName);
        profile.setClientType(effectiveType);
        profile.setPublicPhone(contactPhone != null ? contactPhone.trim() : null);
        profile.setShowPhone(false); // default false for privacy
        profile.setStatus(ClientProfileStatus.ACTIVE);
        profile.setVerified(false);
        profile.setCreatedAt(Instant.now());
        profile.setUpdatedAt(Instant.now());

        return clientProfileRepository.save(profile);
    }

    /**
     * Public endpoint: Get client profile by slug.
     * Enforces strict privacy rules:
     * - Throws 404 if profile is HIDDEN, user is SUSPENDED, or user is not a CLIENT/ADMIN.
     * - Returns public fields only; hides phone unless showPhone is true.
     */
    @Transactional(readOnly = true)
    public PublicClientProfileResponse getPublicProfile(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new ResourceNotFoundException("Client profile not found");
        }

        ClientProfile profile = clientProfileRepository.findBySlug(slug.trim().toLowerCase())
            .orElseThrow(() -> new ResourceNotFoundException("Client profile not found for slug: " + slug));

        // Rule: Hidden profiles are invisible to the public
        if (profile.getStatus() != ClientProfileStatus.ACTIVE) {
            throw new ResourceNotFoundException("This profile is not available");
        }

        // Rule: If user is suspended or no longer CLIENT/ADMIN, hide from public API
        User user = profile.getUser();
        if (user == null || user.getStatus() == UserStatus.SUSPENDED) {
            throw new ResourceNotFoundException("This profile is not available");
        }
        if (user.getRole() != Role.CLIENT && user.getRole() != Role.ADMIN) {
            throw new ResourceNotFoundException("This profile is not available");
        }

        LocalDate today = ZonedDateTime.now(zoneId).toLocalDate();

        long activeCount = listingRepository.countByClientProfileIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            profile.getId(), Status.PUBLISHED, today, today
        );

        long upcomingCount = listingRepository.countByClientProfileIdAndStatusAndStartDateAfter(
            profile.getId(), Status.PUBLISHED, today
        );

        return PublicClientProfileResponse.fromEntity(profile, zoneId, activeCount, upcomingCount);
    }

    /**
     * Public endpoint: Get active or upcoming listings of a client.
     * - "active": running now (startDate <= today <= endDate, status = PUBLISHED)
     * - "upcoming": starts later (startDate > today, status = PUBLISHED)
     * - Expired, draft, pending, or declined listings are NEVER returned.
     */
    @Transactional(readOnly = true)
    public Page<ListingResponse> getClientListings(String slug, String tab, Pageable pageable) {
        // Validate profile exists and is publicly accessible
        PublicClientProfileResponse profile = getPublicProfile(slug);

        LocalDate today = ZonedDateTime.now(zoneId).toLocalDate();
        LocalTime nowTime = ZonedDateTime.now(zoneId).toLocalTime();

        Page<Listing> page;
        if ("upcoming".equalsIgnoreCase(tab)) {
            page = listingRepository.findUpcomingClientListings(profile.getId(), today, pageable);
        } else {
            // Default: active
            page = listingRepository.findActiveClientListings(profile.getId(), today, pageable);
        }

        return page.map(l -> mapListingToResponse(l, profile, nowTime));
    }

    private ListingResponse mapListingToResponse(Listing listing, PublicClientProfileResponse client, LocalTime currentTime) {
        ListingResponse dto = new ListingResponse();
        dto.setId(listing.getId());
        dto.setTitle(listing.getTitle());
        dto.setCategory(listing.getCategory());
        dto.setDescription(listing.getDescription());
        dto.setAddress(listing.getAddress());
        dto.setLandmark(listing.getLandmark());
        dto.setCity(listing.getCity() != null ? listing.getCity() : "Kolkata");
        dto.setLatitude(listing.getLatitude());
        dto.setLongitude(listing.getLongitude());
        dto.setStartDate(listing.getStartDate());
        dto.setEndDate(listing.getEndDate());
        dto.setDailyOpenTime(listing.getDailyOpenTime());
        dto.setDailyCloseTime(listing.getDailyCloseTime());
        dto.setOfferText(listing.getOfferText());
        dto.setContactPhone(listing.getContactPhone());
        dto.setStatus(listing.getStatus());
        dto.setCreatedAt(listing.getCreatedAt());
        dto.setUpdatedAt(listing.getUpdatedAt());

        // Client info linkage
        dto.setClientProfileId(client.getId());
        dto.setClientSlug(client.getSlug());
        dto.setClientBusinessName(client.getBusinessName());
        dto.setClientVerified(client.isVerified());

        // Compute openNow
        dto.setOpenNow(computeOpenNow(listing.getDailyOpenTime(), listing.getDailyCloseTime(), currentTime));

        // Images
        if (listing.getImages() != null && !listing.getImages().isEmpty()) {
            dto.setPrimaryImageUrl(listing.getImages().get(0).getUrl());
            List<ImageResponse> imgs = listing.getImages().stream()
                .map(i -> new ImageResponse(i.getId(), i.getUrl(), i.getSortOrder(), i.getCreatedAt()))
                .collect(Collectors.toList());
            dto.setImages(imgs);
        }

        return dto;
    }

    private boolean computeOpenNow(LocalTime open, LocalTime close, LocalTime current) {
        if (open == null || close == null || current == null) return false;
        if (open.isBefore(close)) {
            return !current.isBefore(open) && current.isBefore(close);
        } else {
            return !current.isBefore(open) || current.isBefore(close);
        }
    }
}
