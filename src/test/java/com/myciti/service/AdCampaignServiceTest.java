package com.myciti.service;

import com.myciti.dto.AdCampaignRequest;
import com.myciti.dto.AdCampaignResponse;
import com.myciti.model.*;
import com.myciti.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdCampaignServiceTest {

    @Mock
    private AdCampaignRepository adCampaignRepository;

    @Mock
    private AdReportRepository adReportRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private UserRepository userRepository;

    private AdCampaignService adCampaignService;

    private User clientUser;
    private User normalUser;
    private Listing publishedListing;
    private Submission userSubmission;

    @BeforeEach
    void setUp() {
        adCampaignService = new AdCampaignService(
            adCampaignRepository,
            adReportRepository,
            listingRepository,
            submissionRepository,
            userRepository
        );

        clientUser = new User("sub-c", "client@test.com", "Client", null);
        clientUser.setId(10L);
        clientUser.setRole(Role.CLIENT);

        normalUser = new User("sub-u", "user@test.com", "User", null);
        normalUser.setId(20L);
        normalUser.setRole(Role.USER);

        publishedListing = new Listing();
        publishedListing.setId(100L);
        publishedListing.setTitle("Grand Puja");
        publishedListing.setStatus(Status.PUBLISHED);
        publishedListing.setLatitude(22.57);
        publishedListing.setLongitude(88.36);
        publishedListing.setCity("Kolkata");

        userSubmission = new Submission();
        userSubmission.setId(500L);
        userSubmission.setUser(clientUser);
        userSubmission.setListingId(100L);
    }

    @Test
    void createCampaign_userRole_throwsAccessDenied() {
        when(userRepository.findById(20L)).thenReturn(Optional.of(normalUser));

        AdCampaignRequest req = new AdCampaignRequest();
        req.setListingId(100L);
        req.setTitle("Sponsored Festival");
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(5));

        assertThrows(AccessDeniedException.class, () ->
            adCampaignService.createCampaign(20L, req)
        );
    }

    @Test
    void createCampaign_clientNotOwnerOfListing_throwsAccessDenied() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));
        when(listingRepository.findById(100L)).thenReturn(Optional.of(publishedListing));

        User otherUser = new User("sub-other", "other@test.com", "Other", null);
        otherUser.setId(99L);
        Submission otherSubmission = new Submission();
        otherSubmission.setUser(otherUser);
        otherSubmission.setListingId(100L);

        when(submissionRepository.findByListingId(100L)).thenReturn(Optional.of(otherSubmission));

        AdCampaignRequest req = new AdCampaignRequest();
        req.setListingId(100L);
        req.setTitle("Unauthorized Ad");
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(5));

        assertThrows(AccessDeniedException.class, () ->
            adCampaignService.createCampaign(10L, req)
        );
    }

    @Test
    void createCampaign_clientOwner_createsPendingCampaign() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));
        when(listingRepository.findById(100L)).thenReturn(Optional.of(publishedListing));
        when(submissionRepository.findByListingId(100L)).thenReturn(Optional.of(userSubmission));
        when(adCampaignRepository.save(any(AdCampaign.class))).thenAnswer(inv -> {
            AdCampaign c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        AdCampaignRequest req = new AdCampaignRequest();
        req.setListingId(100L);
        req.setTitle("Grand Festive Sale");
        req.setHeadline("50% off on all merchandise");
        req.setTargetType(TargetType.RADIUS);
        req.setRadiusKm(5.0);
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(5));

        AdCampaignResponse res = adCampaignService.createCampaign(10L, req);

        assertNotNull(res);
        assertEquals(1L, res.id());
        assertEquals(CampaignStatus.PENDING, res.status());
        assertEquals("Grand Festive Sale", res.title());
    }

    @Test
    void approveCampaign_admin_setsStatusApproved() {
        AdCampaign campaign = new AdCampaign();
        campaign.setId(1L);
        campaign.setListing(publishedListing);
        campaign.setClientUser(clientUser);
        campaign.setStatus(CampaignStatus.PENDING);

        when(adCampaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(adCampaignRepository.save(any(AdCampaign.class))).thenAnswer(inv -> inv.getArgument(0));

        AdCampaignResponse res = adCampaignService.approveCampaignAdmin(1L);

        assertEquals(CampaignStatus.APPROVED, res.status());
        assertNull(res.rejectionReason());
    }

    @Test
    void createCampaign_whenTitleNotProvided_defaultsToHeadline() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));
        when(listingRepository.findById(100L)).thenReturn(Optional.of(publishedListing));
        when(submissionRepository.findByListingId(100L)).thenReturn(Optional.of(userSubmission));
        when(adCampaignRepository.save(any(AdCampaign.class))).thenAnswer(inv -> {
            AdCampaign c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });

        AdCampaignRequest req = new AdCampaignRequest();
        req.setListingId(100L);
        // title is omitted
        req.setHeadline("Headline Becomes Title Fallback");
        req.setTargetType(TargetType.RADIUS);
        req.setRadiusKm(3.0);
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(7));

        AdCampaignResponse res = adCampaignService.createCampaign(10L, req);

        assertNotNull(res);
        assertEquals("Headline Becomes Title Fallback", res.title());
        assertEquals("Headline Becomes Title Fallback", res.headline());
    }

    @Test
    void deleteCampaign_ownerClient_success() {
        AdCampaign campaign = new AdCampaign();
        campaign.setId(5L);
        campaign.setClientUser(clientUser);

        when(adCampaignRepository.findById(5L)).thenReturn(Optional.of(campaign));
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));

        assertDoesNotThrow(() -> adCampaignService.deleteCampaign(10L, 5L));
        verify(adCampaignRepository).delete(campaign);
    }
}
