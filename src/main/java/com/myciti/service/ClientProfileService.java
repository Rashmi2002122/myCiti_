package com.myciti.service;

import com.myciti.dto.ClientProfileResponse;
import com.myciti.dto.ImageResponse;
import com.myciti.dto.ListingResponse;
import com.myciti.dto.PublicClientProfileResponse;
import com.myciti.dto.UpdateClientProfileRequest;
import com.myciti.exception.ResourceNotFoundException;
import com.myciti.model.*;
import com.myciti.repository.ClientProfileRepository;
import com.myciti.repository.ListingRepository;
import com.myciti.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ClientProfileService {

    private static final Pattern INDIAN_PHONE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");

    private final ClientProfileRepository clientProfileRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final SlugService slugService;
    private final StorageService storageService;
    private final com.myciti.repository.ProfileReportRepository profileReportRepository;
    private final ZoneId zoneId;
    private final double minLat;
    private final double maxLat;
    private final double minLng;
    private final double maxLng;

    @Autowired
    public ClientProfileService(
        ClientProfileRepository clientProfileRepository,
        ListingRepository listingRepository,
        UserRepository userRepository,
        SlugService slugService,
        StorageService storageService,
        @Autowired(required = false) com.myciti.repository.ProfileReportRepository profileReportRepository,
        @Value("${app.timezone:Asia/Kolkata}") String timezone,
        @Value("${app.city.bbox.min-lat:6.00}") double minLat,
        @Value("${app.city.bbox.max-lat:37.50}") double maxLat,
        @Value("${app.city.bbox.min-lng:68.00}") double minLng,
        @Value("${app.city.bbox.max-lng:97.50}") double maxLng
    ) {
        this.clientProfileRepository = clientProfileRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.slugService = slugService;
        this.storageService = storageService;
        this.profileReportRepository = profileReportRepository;
        this.zoneId = ZoneId.of(timezone);
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLng = minLng;
        this.maxLng = maxLng;
    }

    /**
     * Backward-compatible constructor for testing without ProfileReportRepository.
     */
    public ClientProfileService(
        ClientProfileRepository clientProfileRepository,
        ListingRepository listingRepository,
        UserRepository userRepository,
        SlugService slugService,
        StorageService storageService,
        String timezone,
        double minLat,
        double maxLat,
        double minLng,
        double maxLng
    ) {
        this(clientProfileRepository, listingRepository, userRepository, slugService, storageService, null, timezone, minLat, maxLat, minLng, maxLng);
    }
    /**
     * Backward-compatible constructor for testing.
     */
    public ClientProfileService(
        ClientProfileRepository clientProfileRepository,
        ListingRepository listingRepository,
        SlugService slugService,
        String timezone
    ) {
        this(clientProfileRepository, listingRepository, null, slugService, null, null, timezone, 6.00, 37.50, 68.00, 97.50);
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
    @Cacheable(value = "clientProfiles", key = "#slug != null ? #slug.trim().toLowerCase() : ''")
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

        long totalCount = listingRepository.countByClientProfileIdAndStatus(profile.getId(), Status.PUBLISHED);

        long activeCount = listingRepository.countByClientProfileIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            profile.getId(), Status.PUBLISHED, today, today
        );

        long upcomingCount = listingRepository.countByClientProfileIdAndStatusAndStartDateAfter(
            profile.getId(), Status.PUBLISHED, today
        );

        long expiredCount = listingRepository.countByClientProfileIdAndStatusAndEndDateBefore(
            profile.getId(), Status.PUBLISHED, today
        );

        return PublicClientProfileResponse.fromEntity(profile, zoneId, totalCount, activeCount, upcomingCount, expiredCount);
    }

    /**
     * Public endpoint: Get active or upcoming listings of a client.
     * - "active": running now (startDate <= today <= endDate, status = PUBLISHED)
     * - "upcoming": starts later (startDate > today, status = PUBLISHED)
     * - Expired, draft, pending, or declined listings are NEVER returned.
     */
    @Transactional(readOnly = true)
    public Page<ListingResponse> getClientListings(String slug, String tab, Pageable pageable) {
        PublicClientProfileResponse profile = getPublicProfile(slug);

        LocalDate today = ZonedDateTime.now(zoneId).toLocalDate();
        LocalTime nowTime = ZonedDateTime.now(zoneId).toLocalTime();

        Page<Listing> page;
        if ("upcoming".equalsIgnoreCase(tab)) {
            page = listingRepository.findUpcomingClientListings(profile.getId(), today, pageable);
        } else if ("active".equalsIgnoreCase(tab) || "live".equalsIgnoreCase(tab)) {
            page = listingRepository.findActiveClientListings(profile.getId(), today, pageable);
        } else if ("expired".equalsIgnoreCase(tab)) {
            page = listingRepository.findExpiredClientListings(profile.getId(), today, pageable);
        } else {
            // "all" or default: return all published listings of this client
            page = listingRepository.findAllClientListings(profile.getId(), pageable);
        }

        return page.map(l -> mapListingToResponse(l, profile, nowTime, today));
    }

    /**
     * Client endpoint: GET /api/me/client-profile.
     * Fetches current client's own profile.
     */
    @Transactional
    public ClientProfileResponse getMyClientProfile(Long userId) {
        User user = getUserOrThrow(userId);
        validateClientOrAdmin(user);

        ClientProfile profile = clientProfileRepository.findByUserId(userId)
            .orElseGet(() -> createProfileForClient(user, user.getBusinessName(), user.getClientType(), user.getPhone()));

        LocalDate today = ZonedDateTime.now(zoneId).toLocalDate();
        long totalCount = listingRepository.countByClientProfileIdAndStatus(profile.getId(), Status.PUBLISHED);
        long activeCount = listingRepository.countByClientProfileIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            profile.getId(), Status.PUBLISHED, today, today
        );
        long upcomingCount = listingRepository.countByClientProfileIdAndStatusAndStartDateAfter(
            profile.getId(), Status.PUBLISHED, today
        );
        long expiredCount = listingRepository.countByClientProfileIdAndStatusAndEndDateBefore(
            profile.getId(), Status.PUBLISHED, today
        );

        return ClientProfileResponse.fromEntity(profile, zoneId, totalCount, activeCount, upcomingCount, expiredCount);
    }

    /**
     * Client endpoint: PUT /api/me/client-profile.
     * Updates client profile details with validation, HTML stripping, and Indian phone sanitization.
     */
    @CacheEvict(value = "clientProfiles", allEntries = true)
    @Transactional
    public ClientProfileResponse updateMyClientProfile(Long userId, UpdateClientProfileRequest req) {
        User user = getUserOrThrow(userId);
        validateClientOrAdmin(user);

        ClientProfile profile = clientProfileRepository.findByUserId(userId)
            .orElseGet(() -> createProfileForClient(user, user.getBusinessName(), user.getClientType(), user.getPhone()));

        // Business Name: validated, HTML stripped, slug remains immutable
        if (req.getBusinessName() != null) {
            String cleanName = sanitize(req.getBusinessName(), 120);
            if (cleanName.isBlank()) {
                throw new IllegalArgumentException("Business name cannot be blank");
            }
            profile.setBusinessName(cleanName);
            user.setBusinessName(cleanName);
        }

        // Client Type: validated
        if (req.getClientType() != null) {
            profile.setClientType(req.getClientType());
            user.setClientType(req.getClientType());
        }

        // Bio: max 160, HTML stripped
        if (req.getBio() != null) {
            String cleanBio = sanitize(req.getBio(), 160);
            profile.setBio(cleanBio);
        }

        // Address: max 500, HTML stripped
        if (req.getAddress() != null) {
            profile.setAddress(sanitize(req.getAddress(), 500));
        }

        // Landmark: max 255, HTML stripped
        if (req.getLandmark() != null) {
            profile.setLandmark(sanitize(req.getLandmark(), 255));
        }

        // Location coordinates bounding box validation
        if (req.getLatitude() != null || req.getLongitude() != null) {
            if (req.getLatitude() == null || req.getLongitude() == null) {
                throw new IllegalArgumentException("Both latitude and longitude must be provided together");
            }
            double lat = req.getLatitude();
            double lng = req.getLongitude();
            if (lat < minLat || lat > maxLat || lng < minLng || lng > maxLng) {
                throw new IllegalArgumentException(String.format(
                    "Location coordinates (%.4f, %.4f) are outside the supported boundary [%.2f-%.2f Lat, %.2f-%.2f Lng]",
                    lat, lng, minLat, maxLat, minLng, maxLng
                ));
            }
            profile.setLatitude(lat);
            profile.setLongitude(lng);
        }

        // Phone numbers: Indian format validation & independent from login profile
        if (req.getPublicPhone() != null) {
            profile.setPublicPhone(validateAndCleanPhone(req.getPublicPhone(), "Public phone"));
        }

        if (req.getShowPhone() != null) {
            profile.setShowPhone(req.getShowPhone());
        }

        if (req.getWhatsappNumber() != null) {
            profile.setWhatsappNumber(validateAndCleanPhone(req.getWhatsappNumber(), "WhatsApp number"));
        }

        // Operating hours
        if (req.getDailyOpenTime() != null) {
            profile.setDailyOpenTime(req.getDailyOpenTime());
        }

        if (req.getDailyCloseTime() != null) {
            profile.setDailyCloseTime(req.getDailyCloseTime());
        }

        profile.setUpdatedAt(Instant.now());
        ClientProfile saved = clientProfileRepository.save(profile);

        LocalDate today = ZonedDateTime.now(zoneId).toLocalDate();
        long activeCount = listingRepository.countByClientProfileIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            saved.getId(), Status.PUBLISHED, today, today
        );
        long upcomingCount = listingRepository.countByClientProfileIdAndStatusAndStartDateAfter(
            saved.getId(), Status.PUBLISHED, today
        );

        return ClientProfileResponse.fromEntity(saved, zoneId, activeCount, upcomingCount);
    }

    /**
     * Client endpoint: POST /api/me/client-profile/logo.
     * Uploads business logo (max 5 MB, resized to 400px, EXIF stripped).
     */
    @CacheEvict(value = "clientProfiles", allEntries = true)
    @Transactional
    public String updateLogo(Long userId, MultipartFile file) throws IOException {
        User user = getUserOrThrow(userId);
        validateClientOrAdmin(user);

        if (storageService == null) {
            throw new IllegalStateException("Storage service is not configured");
        }

        ClientProfile profile = clientProfileRepository.findByUserId(userId)
            .orElseGet(() -> createProfileForClient(user, user.getBusinessName(), user.getClientType(), user.getPhone()));

        String oldLogo = profile.getLogoUrl();
        String newUrl = storageService.storeClientLogo(file);

        profile.setLogoUrl(newUrl);
        profile.setUpdatedAt(Instant.now());
        clientProfileRepository.save(profile);

        if (oldLogo != null && !oldLogo.isBlank() && !oldLogo.equals(newUrl)) {
            storageService.deleteImage(oldLogo);
        }

        return newUrl;
    }

    /**
     * Client endpoint: POST /api/me/client-profile/cover.
     * Uploads business cover banner (max 5 MB, resized to 1200px wide, EXIF stripped).
     */
    @CacheEvict(value = "clientProfiles", allEntries = true)
    @Transactional
    public String updateCover(Long userId, MultipartFile file) throws IOException {
        User user = getUserOrThrow(userId);
        validateClientOrAdmin(user);

        if (storageService == null) {
            throw new IllegalStateException("Storage service is not configured");
        }

        ClientProfile profile = clientProfileRepository.findByUserId(userId)
            .orElseGet(() -> createProfileForClient(user, user.getBusinessName(), user.getClientType(), user.getPhone()));

        String oldCover = profile.getCoverUrl();
        String newUrl = storageService.storeClientCover(file);

        profile.setCoverUrl(newUrl);
        profile.setUpdatedAt(Instant.now());
        clientProfileRepository.save(profile);

        if (oldCover != null && !oldCover.isBlank() && !oldCover.equals(newUrl)) {
            storageService.deleteImage(oldCover);
        }

        return newUrl;
    }

    private User getUserOrThrow(Long userId) {
        if (userRepository == null) {
            throw new IllegalStateException("User repository is not configured");
        }
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    private void validateClientOrAdmin(User user) {
        if (user.getRole() != Role.CLIENT && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only client accounts can manage client profiles.");
        }
    }

    private String validateAndCleanPhone(String rawPhone, String fieldName) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }
        String cleaned = rawPhone.trim().replaceAll("^(\\+91|\\+91-|\\+91\\s|0)", "").replaceAll("[\\s\\-\\(\\)]+", "");
        if (!INDIAN_PHONE_PATTERN.matcher(cleaned).matches()) {
            throw new IllegalArgumentException(fieldName + " must be a valid 10-digit Indian mobile number");
        }
        return cleaned;
    }

    private String sanitize(String input, int maxLength) {
        if (input == null) return null;
        String stripped = input.replaceAll("<[^>]*>", "").trim();
        if (stripped.length() > maxLength) {
            return stripped.substring(0, maxLength);
        }
        return stripped;
    }

    private ListingResponse mapListingToResponse(Listing listing, PublicClientProfileResponse client, LocalTime currentTime) {
        return mapListingToResponse(listing, client, currentTime, LocalDate.now(zoneId));
    }

    private ListingResponse mapListingToResponse(Listing listing, PublicClientProfileResponse client, LocalTime currentTime, LocalDate today) {
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
        dto.setClientLogoUrl(client.getLogoUrl());
        dto.setClientType(client.getClientType() != null ? client.getClientType().name() : null);

        boolean expired = listing.getEndDate() != null && listing.getEndDate().isBefore(today);
        dto.setIsExpired(expired);

        if (expired) {
            dto.setOpenNow(false);
        } else if (listing.getStartDate() != null && listing.getStartDate().isAfter(today)) {
            dto.setOpenNow(false);
        } else if (listing.getDailyOpenTime() != null && listing.getDailyCloseTime() != null) {
            LocalTime open = listing.getDailyOpenTime();
            LocalTime close = listing.getDailyCloseTime();
            if (open.isBefore(close)) {
                dto.setOpenNow(!currentTime.isBefore(open) && currentTime.isBefore(close));
            } else {
                dto.setOpenNow(!currentTime.isBefore(open) || currentTime.isBefore(close));
            }
        } else {
            dto.setOpenNow(true);
        }

        if (listing.getImages() != null) {
            List<ImageResponse> images = listing.getImages().stream()
                .map(img -> new ImageResponse(img.getId(), img.getUrl(), img.getSortOrder()))
                .collect(Collectors.toList());
            dto.setImages(images);
            if (!images.isEmpty()) {
                dto.setPrimaryImageUrl(images.get(0).getUrl());
            }
        }

        return dto;
    }

    @Transactional
    public void reportProfile(String slug, Long reporterUserId, com.myciti.dto.ProfileReportRequest req) {
        if (slug == null || slug.isBlank()) {
            throw new ResourceNotFoundException("Client profile not found");
        }
        ClientProfile profile = clientProfileRepository.findBySlug(slug.trim().toLowerCase())
            .orElseThrow(() -> new ResourceNotFoundException("Client profile not found for slug: " + slug));

        if (profileReportRepository != null) {
            ProfileReport report = new ProfileReport(profile, reporterUserId, req.getReason(), req.getDetails());
            profileReportRepository.save(report);
        }
    }

    @Transactional(readOnly = true)
    public Page<com.myciti.dto.ProfileReportDto> getProfileReportsAdmin(ProfileReportStatus status, Pageable pageable) {
        if (profileReportRepository == null) {
            return Page.empty(pageable);
        }
        Page<ProfileReport> page = (status != null)
            ? profileReportRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
            : profileReportRepository.findAll(pageable);
        return page.map(com.myciti.dto.ProfileReportDto::fromEntity);
    }
}