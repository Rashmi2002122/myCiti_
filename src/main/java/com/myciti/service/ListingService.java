package com.myciti.service;

import com.myciti.dto.ImageResponse;
import com.myciti.dto.ListingResponse;
import com.myciti.exception.ResourceNotFoundException;
import com.myciti.model.Category;
import com.myciti.model.ClientProfile;
import com.myciti.model.Listing;
import com.myciti.model.ListingImage;
import com.myciti.repository.ClientProfileRepository;
import com.myciti.repository.ListingImageRepository;
import com.myciti.repository.ListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ListingService {

    private final ListingRepository listingRepository;
    private final ListingImageRepository listingImageRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final ZoneId kolkataZone;

    @Autowired
    public ListingService(
        ListingRepository listingRepository,
        ListingImageRepository listingImageRepository,
        ClientProfileRepository clientProfileRepository,
        @Value("${app.timezone:Asia/Kolkata}") String timeZone
    ) {
        this.listingRepository = listingRepository;
        this.listingImageRepository = listingImageRepository;
        this.clientProfileRepository = clientProfileRepository;
        this.kolkataZone = ZoneId.of(timeZone);
    }

    public ListingService(
        ListingRepository listingRepository,
        ListingImageRepository listingImageRepository,
        @Value("${app.timezone:Asia/Kolkata}") String timeZone
    ) {
        this(listingRepository, listingImageRepository, null, timeZone);
    }

    /**
     * Fetch active listings with optional distance, category and query filters.
     * Caches non-coordinate queries (e.g. city/category landing views) in memory.
     */
    @Cacheable(value = "listings", key = "{#category, #city, #lat, #lng, #radiusKm, #q}", condition = "#lat == null && #lng == null")
    @Transactional(readOnly = true)
    public List<ListingResponse> getListings(
        Category category,
        String city,
        Double lat,
        Double lng,
        Double radiusKm,
        String q
    ) {
        ZonedDateTime nowInKolkata = ZonedDateTime.now(kolkataZone);
        LocalDate today = nowInKolkata.toLocalDate();
        LocalTime currentTime = nowInKolkata.toLocalTime();

        String trimmedQ = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
        if (trimmedQ != null && trimmedQ.length() > 100) {
            trimmedQ = trimmedQ.substring(0, 100);
        }
        String categoryStr = (category != null) ? category.name() : null;
        String cityStr = (city != null && !city.trim().isEmpty() && !city.equalsIgnoreCase("ALL") && !city.equalsIgnoreCase("All Odisha")) ? city.trim() : null;

        if (lat != null && lng != null) {
            List<Object[]> rows = listingRepository.findActiveWithDistance(
                today,
                lat,
                lng,
                radiusKm,
                categoryStr,
                cityStr,
                trimmedQ
            );

            // Fetch images in a single batch query for listed IDs to avoid N+1 queries and memory exhaustion
            List<Long> listingIds = new ArrayList<>();
            for (Object[] row : rows) {
                listingIds.add(((Number) row[0]).longValue());
            }

            Map<Long, List<ListingImage>> imagesByListingId = listingIds.isEmpty() ? Map.of() :
                listingImageRepository.findByListingIdInOrderBySortOrderAsc(listingIds).stream()
                    .collect(Collectors.groupingBy(img -> img.getListing().getId()));

            List<ListingResponse> responses = new ArrayList<>();
            for (Object[] row : rows) {
                ListingResponse response = mapNativeRowToResponse(row, currentTime);
                List<ListingImage> imgs = imagesByListingId.getOrDefault(response.getId(), List.of());
                setImagesOnResponse(response, imgs);
                responses.add(response);
            }

            // Batch enrich client business profile metadata
            enrichClientProfilesBatch(responses);

            return deduplicateClientListings(responses);
        } else {
            List<Listing> listings = listingRepository.findActiveWithoutDistance(today, categoryStr, cityStr, trimmedQ);
            if (listings.isEmpty()) {
                return List.of();
            }

            List<Long> listingIds = listings.stream().map(Listing::getId).collect(Collectors.toList());
            Map<Long, List<ListingImage>> imagesByListingId = listingImageRepository
                .findByListingIdInOrderBySortOrderAsc(listingIds).stream()
                .collect(Collectors.groupingBy(img -> img.getListing().getId()));

            List<ListingResponse> responses = listings.stream()
                .map(listing -> {
                    ListingResponse response = mapEntityToResponse(listing, currentTime, null);
                    List<ListingImage> imgs = imagesByListingId.getOrDefault(listing.getId(), List.of());
                    setImagesOnResponse(response, imgs);
                    return response;
                })
                .collect(Collectors.toList());

            // Batch enrich client business profile metadata
            enrichClientProfilesBatch(responses);

            return deduplicateClientListings(responses);
        }
    }

    /**
     * Get single listing detail by ID.
     * Non-PUBLISHED listings are restricted to the owner client or an ADMIN.
     */
    @Cacheable(value = "listingById", key = "#id", unless = "#result == null || #result.status != T(com.myciti.model.Status).PUBLISHED")
    @Transactional(readOnly = true)
    public ListingResponse getListingById(Long id) {
        Listing listing = listingRepository.findByIdWithImages(id)
            .orElseThrow(() -> new ResourceNotFoundException("Listing with id " + id + " not found"));

        if (listing.getStatus() != com.myciti.model.Status.PUBLISHED) {
            if (!canViewUnpublishedListing(listing)) {
                throw new ResourceNotFoundException("Listing with id " + id + " not found");
            }
        }

        ZonedDateTime nowInKolkata = ZonedDateTime.now(kolkataZone);
        ListingResponse response = mapEntityToResponse(listing, nowInKolkata.toLocalTime(), null);
        setImagesOnResponse(response, listing.getImages());
        enrichClientProfile(response, listing.getClientProfileId());
        return response;
    }

    private boolean canViewUnpublishedListing(Listing listing) {
        try {
            org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return false;
            }
            if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
                return true;
            }
            if (auth.getPrincipal() instanceof com.myciti.model.User currentUser) {
                if (listing.getClientProfileId() != null && clientProfileRepository != null) {
                    return clientProfileRepository.findById(listing.getClientProfileId())
                        .map(cp -> cp.getUser() != null && cp.getUser().getId().equals(currentUser.getId()))
                        .orElse(false);
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Compute whether a listing is currently open in Asia/Kolkata timezone.
     */
    public boolean computeOpenNow(LocalTime openTime, LocalTime closeTime, LocalTime currentTime) {
        if (openTime == null || closeTime == null || currentTime == null) {
            return false;
        }

        // Standard case: e.g. 09:00 to 22:00
        if (openTime.isBefore(closeTime)) {
            return !currentTime.isBefore(openTime) && !currentTime.isAfter(closeTime);
        }
        // Overnight case: e.g. 08:00 to 02:00 (past midnight next morning)
        else if (openTime.isAfter(closeTime)) {
            return !currentTime.isBefore(openTime) || !currentTime.isAfter(closeTime);
        }
        // 24-hour open case: e.g. 00:00 to 00:00
        else {
            return true;
        }
    }

    /**
     * Deduplicate listings by clientProfileId so that a client with multiple posts
     * only displays their most recent post on the map and explore feeds.
     * Standalone listings (null clientProfileId) are preserved.
     * The retained recent post is tagged with hasOtherUpdates and otherUpdatesCount.
     */
    private List<ListingResponse> deduplicateClientListings(List<ListingResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return responses;
        }

        Map<Long, List<ListingResponse>> clientGroups = new HashMap<>();
        for (ListingResponse resp : responses) {
            if (resp.getClientProfileId() != null) {
                clientGroups.computeIfAbsent(resp.getClientProfileId(), k -> new ArrayList<>()).add(resp);
            }
        }

        Set<Long> olderListingIdsToExclude = new HashSet<>();
        for (Map.Entry<Long, List<ListingResponse>> entry : clientGroups.entrySet()) {
            List<ListingResponse> group = entry.getValue();
            if (group.size() > 1) {
                // Determine the most recent listing by createdAt desc, fallback to id desc
                ListingResponse newest = group.get(0);
                for (int i = 1; i < group.size(); i++) {
                    ListingResponse candidate = group.get(i);
                    if (isNewer(candidate, newest)) {
                        newest = candidate;
                    }
                }
                newest.setHasOtherUpdates(true);
                newest.setOtherUpdatesCount(group.size() - 1);

                for (ListingResponse item : group) {
                    if (!Objects.equals(item.getId(), newest.getId())) {
                        olderListingIdsToExclude.add(item.getId());
                    }
                }
            }
        }

        if (olderListingIdsToExclude.isEmpty()) {
            return responses;
        }

        List<ListingResponse> result = new ArrayList<>(responses.size());
        for (ListingResponse resp : responses) {
            if (!olderListingIdsToExclude.contains(resp.getId())) {
                result.add(resp);
            }
        }
        return result;
    }

    private boolean isNewer(ListingResponse candidate, ListingResponse current) {
        if (candidate.getCreatedAt() != null && current.getCreatedAt() != null) {
            if (candidate.getCreatedAt().isAfter(current.getCreatedAt())) {
                return true;
            }
            if (current.getCreatedAt().isAfter(candidate.getCreatedAt())) {
                return false;
            }
        }
        long candId = candidate.getId() != null ? candidate.getId() : 0L;
        long currId = current.getId() != null ? current.getId() : 0L;
        return candId > currId;
    }

    private void setImagesOnResponse(ListingResponse response, List<ListingImage> images) {
        if (images != null && !images.isEmpty()) {
            List<ImageResponse> imageResponses = images.stream()
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .map(img -> new ImageResponse(img.getId(), img.getUrl(), img.getSortOrder(), img.getCreatedAt()))
                .collect(Collectors.toList());
            response.setImages(imageResponses);
            response.setPrimaryImageUrl(imageResponses.get(0).getUrl());
        } else {
            response.setImages(List.of());
            response.setPrimaryImageUrl(null);
        }
    }

    private void enrichClientProfile(ListingResponse response, Long clientProfileId) {
        if (response == null || clientProfileId == null || clientProfileRepository == null) {
            return;
        }
        clientProfileRepository.findById(clientProfileId).ifPresent(cp -> {
            response.setClientProfileId(cp.getId());
            response.setClientSlug(cp.getSlug());
            response.setClientBusinessName(cp.getBusinessName());
            response.setClientLogoUrl(cp.getLogoUrl());
            response.setClientVerified(cp.isVerified());
            response.setClientType(cp.getClientType() != null ? cp.getClientType().name() : null);
        });
    }

    private void enrichClientProfilesBatch(List<ListingResponse> responses) {
        if (responses == null || responses.isEmpty() || clientProfileRepository == null) {
            return;
        }
        Set<Long> profileIds = responses.stream()
            .map(ListingResponse::getClientProfileId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (profileIds.isEmpty()) {
            return;
        }
        Map<Long, ClientProfile> profiles = clientProfileRepository.findAllById(profileIds).stream()
            .collect(Collectors.toMap(ClientProfile::getId, Function.identity(), (a, b) -> a));
        for (ListingResponse resp : responses) {
            if (resp.getClientProfileId() != null) {
                ClientProfile cp = profiles.get(resp.getClientProfileId());
                if (cp != null) {
                    resp.setClientSlug(cp.getSlug());
                    resp.setClientBusinessName(cp.getBusinessName());
                    resp.setClientLogoUrl(cp.getLogoUrl());
                    resp.setClientVerified(cp.isVerified());
                    resp.setClientType(cp.getClientType() != null ? cp.getClientType().name() : null);
                }
            }
        }
    }

    private ListingResponse mapEntityToResponse(Listing listing, LocalTime currentTime, Double distanceKm) {
        ListingResponse dto = new ListingResponse();
        dto.setId(listing.getId());
        dto.setTitle(listing.getTitle());
        dto.setCategory(listing.getCategory());
        dto.setDescription(listing.getDescription());
        dto.setAddress(listing.getAddress());
        dto.setLandmark(listing.getLandmark());
        dto.setCity(listing.getCity() != null ? listing.getCity() : "Bhubaneswar");
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
        dto.setClientProfileId(listing.getClientProfileId());
        dto.setDistanceKm(distanceKm != null ? roundTwoDecimals(distanceKm) : null);
        LocalDate today = ZonedDateTime.now(kolkataZone).toLocalDate();
        boolean expired = listing.getEndDate() != null && listing.getEndDate().isBefore(today);
        dto.setIsExpired(expired);
        if (expired) {
            dto.setOpenNow(false);
        } else {
            dto.setOpenNow(computeOpenNow(listing.getDailyOpenTime(), listing.getDailyCloseTime(), currentTime));
        }
        return dto;
    }

    private ListingResponse mapNativeRowToResponse(Object[] row, LocalTime currentTime) {
        ListingResponse dto = new ListingResponse();
        // Column indices match:
        // 0: id, 1: title, 2: category, 3: description, 4: address, 5: landmark,
        // 6: city, 7: latitude, 8: longitude, 9: start_date, 10: end_date, 11: daily_open_time,
        // 12: daily_close_time, 13: offer_text, 14: contact_phone, 15: status,
        // 16: created_at, 17: updated_at, 18: distance_km, 19: client_profile_id
        dto.setId(((Number) row[0]).longValue());
        dto.setTitle((String) row[1]);
        dto.setCategory(Category.valueOf((String) row[2]));
        dto.setDescription((String) row[3]);
        dto.setAddress((String) row[4]);
        dto.setLandmark((String) row[5]);
        dto.setCity(row[6] != null ? (String) row[6] : "Bhubaneswar");
        dto.setLatitude(((Number) row[7]).doubleValue());
        dto.setLongitude(((Number) row[8]).doubleValue());

        if (row[9] instanceof Date) {
            dto.setStartDate(((Date) row[9]).toLocalDate());
        }
        if (row[10] instanceof Date) {
            dto.setEndDate(((Date) row[10]).toLocalDate());
        }
        if (row[11] instanceof Time) {
            dto.setDailyOpenTime(((Time) row[11]).toLocalTime());
        }
        if (row[12] instanceof Time) {
            dto.setDailyCloseTime(((Time) row[12]).toLocalTime());
        }

        dto.setOfferText((String) row[13]);
        dto.setContactPhone((String) row[14]);
        dto.setStatus(com.myciti.model.Status.valueOf((String) row[15]));

        if (row[16] instanceof Timestamp) {
            dto.setCreatedAt(((Timestamp) row[16]).toInstant());
        }
        if (row[17] instanceof Timestamp) {
            dto.setUpdatedAt(((Timestamp) row[17]).toInstant());
        }

        if (row.length > 18 && row[18] != null) {
            double distance = ((Number) row[18]).doubleValue();
            dto.setDistanceKm(roundTwoDecimals(distance));
        }

        if (row.length > 19 && row[19] != null) {
            dto.setClientProfileId(((Number) row[19]).longValue());
        }

        LocalDate today = ZonedDateTime.now(kolkataZone).toLocalDate();
        boolean expired = dto.getEndDate() != null && dto.getEndDate().isBefore(today);
        dto.setIsExpired(expired);
        if (expired) {
            dto.setOpenNow(false);
        } else {
            dto.setOpenNow(computeOpenNow(dto.getDailyOpenTime(), dto.getDailyCloseTime(), currentTime));
        }
        return dto;
    }

    private double roundTwoDecimals(double val) {
        return BigDecimal.valueOf(val).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Admin: Fetch all listings regardless of status or date.
     */
    @Transactional(readOnly = true)
    public List<ListingResponse> getAllListingsForAdmin() {
        ZonedDateTime nowInKolkata = ZonedDateTime.now(kolkataZone);
        LocalTime currentTime = nowInKolkata.toLocalTime();
        List<ListingResponse> responses = listingRepository.findAll().stream()
            .map(l -> {
                ListingResponse res = mapEntityToResponse(l, currentTime, null);
                setImagesOnResponse(res, l.getImages());
                return res;
            })
            .collect(Collectors.toList());

        enrichClientProfilesBatch(responses);
        return responses;
    }

    /**
     * Admin: Create new listing.
     */
    @Caching(evict = {
        @CacheEvict(value = "listings", allEntries = true),
        @CacheEvict(value = "listingById", allEntries = true)
    })
    @Transactional
    public ListingResponse createListing(com.myciti.dto.ListingRequest req) {
        Listing listing = new Listing();
        copyProperties(req, listing);
        Listing saved = listingRepository.save(listing);
        ZonedDateTime nowInKolkata = ZonedDateTime.now(kolkataZone);
        ListingResponse res = mapEntityToResponse(saved, nowInKolkata.toLocalTime(), null);
        enrichClientProfile(res, saved.getClientProfileId());
        return res;
    }

    /**
     * Admin: Update existing listing.
     */
    @Caching(evict = {
        @CacheEvict(value = "listings", allEntries = true),
        @CacheEvict(value = "listingById", key = "#id")
    })
    @Transactional
    public ListingResponse updateListing(Long id, com.myciti.dto.ListingRequest req) {
        Listing listing = listingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Listing #" + id + " not found"));
        copyProperties(req, listing);
        Listing updated = listingRepository.save(listing);
        ZonedDateTime nowInKolkata = ZonedDateTime.now(kolkataZone);
        ListingResponse res = mapEntityToResponse(updated, nowInKolkata.toLocalTime(), null);
        setImagesOnResponse(res, updated.getImages());
        enrichClientProfile(res, updated.getClientProfileId());
        return res;
    }

    /**
     * Admin: Delete listing.
     */
    @Caching(evict = {
        @CacheEvict(value = "listings", allEntries = true),
        @CacheEvict(value = "listingById", key = "#id")
    })
    @Transactional
    public void deleteListing(Long id) {
        if (!listingRepository.existsById(id)) {
            throw new ResourceNotFoundException("Listing #" + id + " not found");
        }
        listingRepository.deleteById(id);
    }

    /**
     * Admin: Add image to listing.
     */
    @Caching(evict = {
        @CacheEvict(value = "listings", allEntries = true),
        @CacheEvict(value = "listingById", key = "#listingId")
    })
    @Transactional
    public ImageResponse addListingImage(Long listingId, String imageUrl, Integer sortOrder) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new ResourceNotFoundException("Listing #" + listingId + " not found"));

        int order = (sortOrder != null) ? sortOrder : listing.getImages().size() + 1;
        ListingImage image = new ListingImage(listing, imageUrl, order);
        listing.addImage(image);
        ListingImage savedImage = listingImageRepository.save(image);

        return new ImageResponse(savedImage.getId(), savedImage.getUrl(), savedImage.getSortOrder(), savedImage.getCreatedAt());
    }

    private void copyProperties(com.myciti.dto.ListingRequest req, Listing listing) {
        listing.setTitle(req.getTitle());
        listing.setCategory(req.getCategory());
        listing.setDescription(req.getDescription());
        listing.setAddress(req.getAddress());
        listing.setLandmark(req.getLandmark());
        listing.setCity(req.getCity() != null && !req.getCity().trim().isEmpty() ? req.getCity().trim() : "Kolkata");
        listing.setLatitude(req.getLatitude());
        listing.setLongitude(req.getLongitude());
        listing.setStartDate(req.getStartDate());
        listing.setEndDate(req.getEndDate());
        listing.setDailyOpenTime(req.getDailyOpenTime());
        listing.setDailyCloseTime(req.getDailyCloseTime());
        listing.setOfferText(req.getOfferText());
        listing.setContactPhone(req.getContactPhone());
        listing.setStatus(req.getStatus() != null ? req.getStatus() : com.myciti.model.Status.DRAFT);
    }

    public ListingResponse toResponse(Listing listing) {
        if (listing == null) return null;
        java.time.ZonedDateTime nowInKolkata = java.time.ZonedDateTime.now(kolkataZone);
        ListingResponse res = mapEntityToResponse(listing, nowInKolkata.toLocalTime(), null);
        setImagesOnResponse(res, listing.getImages());
        enrichClientProfile(res, listing.getClientProfileId());
        return res;
    }

    /**
     * Periodically evict public listings cache every 15 minutes to keep openNow statuses
     * and day boundary transitions fresh across the day.
     */
    @Scheduled(fixedRate = 900000)
    @CacheEvict(value = "listings", allEntries = true)
    public void evictListingsCachePeriodically() {
        // Evicts cached public search results so real-time openNow stays accurate
    }
}
