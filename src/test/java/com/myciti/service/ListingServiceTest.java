package com.myciti.service;

import com.myciti.dto.ListingResponse;
import com.myciti.model.Category;
import com.myciti.model.ClientProfile;
import com.myciti.model.ClientType;
import com.myciti.model.Listing;
import com.myciti.model.Status;
import com.myciti.repository.ClientProfileRepository;
import com.myciti.repository.ListingImageRepository;
import com.myciti.repository.ListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ListingImageRepository listingImageRepository;

    @Mock
    private ClientProfileRepository clientProfileRepository;

    private ListingService listingService;

    @BeforeEach
    void setUp() {
        listingService = new ListingService(
            listingRepository,
            listingImageRepository,
            clientProfileRepository,
            "Asia/Kolkata"
        );
    }

    @Test
    @DisplayName("getListings: enriches listings with client business profile metadata")
    void testGetListings_EnrichesClientProfile() {
        Listing listing = new Listing();
        listing.setId(10L);
        listing.setTitle("Bagbazar Grand Pandal");
        listing.setCategory(Category.PANDAL);
        listing.setDescription("Historic puja celebration");
        listing.setAddress("Bagbazar, Kolkata");
        listing.setLandmark("Near Ghat");
        listing.setLatitude(22.60);
        listing.setLongitude(88.36);
        listing.setStartDate(LocalDate.now().minusDays(1));
        listing.setEndDate(LocalDate.now().plusDays(5));
        listing.setDailyOpenTime(LocalTime.of(8, 0));
        listing.setDailyCloseTime(LocalTime.of(2, 0));
        listing.setStatus(Status.PUBLISHED);
        listing.setClientProfileId(100L);

        when(listingRepository.findActiveWithoutDistance(any(), any(), any(), any()))
            .thenReturn(List.of(listing));
        when(listingImageRepository.findByListingIdInOrderBySortOrderAsc(any()))
            .thenReturn(List.of());

        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setId(100L);
        clientProfile.setSlug("bagbazar-committee");
        clientProfile.setBusinessName("Bagbazar Sarbojanin Committee");
        clientProfile.setLogoUrl("https://example.com/bagbazar-logo.png");
        clientProfile.setVerified(true);
        clientProfile.setClientType(ClientType.COMPANY);

        when(clientProfileRepository.findAllById(any()))
            .thenReturn(List.of(clientProfile));

        List<ListingResponse> responses = listingService.getListings(null, null, null, null, null, null);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        ListingResponse resp = responses.get(0);
        assertEquals(10L, resp.getId());
        assertEquals("bagbazar-committee", resp.getClientSlug());
        assertEquals("Bagbazar Sarbojanin Committee", resp.getClientBusinessName());
        assertEquals("https://example.com/bagbazar-logo.png", resp.getClientLogoUrl());
        assertTrue(resp.getClientVerified());
        assertEquals("COMPANY", resp.getClientType());
    }

    @Test
    @DisplayName("getListingById: enriches single listing detail with client profile")
    void testGetListingById_EnrichesClientProfile() {
        Listing listing = new Listing();
        listing.setId(20L);
        listing.setTitle("Central Park Crafts Mela");
        listing.setCategory(Category.MELA);
        listing.setDescription("Handicrafts and books");
        listing.setAddress("Salt Lake Sector 2");
        listing.setLandmark("Karunamoyee");
        listing.setLatitude(22.58);
        listing.setLongitude(88.42);
        listing.setStartDate(LocalDate.now().minusDays(1));
        listing.setEndDate(LocalDate.now().plusDays(5));
        listing.setDailyOpenTime(LocalTime.of(10, 0));
        listing.setDailyCloseTime(LocalTime.of(22, 0));
        listing.setStatus(Status.PUBLISHED);
        listing.setClientProfileId(200L);

        when(listingRepository.findByIdWithImages(20L))
            .thenReturn(Optional.of(listing));

        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setId(200L);
        clientProfile.setSlug("crafts-guild");
        clientProfile.setBusinessName("Bengal Crafts Guild");
        clientProfile.setLogoUrl("https://example.com/crafts-logo.png");
        clientProfile.setVerified(true);
        clientProfile.setClientType(ClientType.STORE);

        when(clientProfileRepository.findById(200L))
            .thenReturn(Optional.of(clientProfile));

        ListingResponse resp = listingService.getListingById(20L);

        assertNotNull(resp);
        assertEquals(20L, resp.getId());
        assertEquals("crafts-guild", resp.getClientSlug());
        assertEquals("Bengal Crafts Guild", resp.getClientBusinessName());
        assertEquals("https://example.com/crafts-logo.png", resp.getClientLogoUrl());
        assertTrue(resp.getClientVerified());
        assertEquals("STORE", resp.getClientType());
    }

    @Test
    @DisplayName("getListingById: draft listing requested by anonymous user throws ResourceNotFoundException")
    void testGetListingById_DraftListing_Anonymous_ThrowsNotFound() {
        Listing listing = new Listing();
        listing.setId(99L);
        listing.setTitle("Secret Draft Listing");
        listing.setStatus(Status.DRAFT);

        when(listingRepository.findByIdWithImages(99L)).thenReturn(Optional.of(listing));

        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        assertThrows(com.myciti.exception.ResourceNotFoundException.class, () ->
            listingService.getListingById(99L)
        );
    }

    @Test
    @DisplayName("toResponse: enriches response when called for saved listings")
    void testToResponse_EnrichesClientProfile() {
        Listing listing = new Listing();
        listing.setId(30L);
        listing.setTitle("Kalakriti Festive Launch");
        listing.setCategory(Category.INAUGURATION);
        listing.setDescription("Designer boutique launch");
        listing.setAddress("City Centre 1");
        listing.setLandmark("Kund Area");
        listing.setLatitude(22.58);
        listing.setLongitude(88.40);
        listing.setStartDate(LocalDate.now().minusDays(1));
        listing.setEndDate(LocalDate.now().plusDays(5));
        listing.setDailyOpenTime(LocalTime.of(11, 0));
        listing.setDailyCloseTime(LocalTime.of(21, 0));
        listing.setStatus(Status.PUBLISHED);
        listing.setClientProfileId(300L);

        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setId(300L);
        clientProfile.setSlug("kalakriti-boutique");
        clientProfile.setBusinessName("Kalakriti Designer Boutique");
        clientProfile.setLogoUrl("https://example.com/kalakriti.png");
        clientProfile.setVerified(true);
        clientProfile.setClientType(ClientType.STORE);

        when(clientProfileRepository.findById(300L))
            .thenReturn(Optional.of(clientProfile));

        ListingResponse resp = listingService.toResponse(listing);

        assertNotNull(resp);
        assertEquals(30L, resp.getId());
        assertEquals("kalakriti-boutique", resp.getClientSlug());
        assertEquals("Kalakriti Designer Boutique", resp.getClientBusinessName());
        assertEquals("https://example.com/kalakriti.png", resp.getClientLogoUrl());
        assertTrue(resp.getClientVerified());
    }
}
