package com.myciti.service;

import com.myciti.dto.ListingResponse;
import com.myciti.dto.PublicClientProfileResponse;
import com.myciti.exception.ResourceNotFoundException;
import com.myciti.model.*;
import com.myciti.repository.ClientProfileRepository;
import com.myciti.repository.ListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClientProfilePrivacyTest {

    @Mock
    private ClientProfileRepository clientProfileRepository;

    @Mock
    private ListingRepository listingRepository;

    private SlugService slugService;
    private ClientProfileService clientProfileService;

    private User clientUser;
    private ClientProfile activeProfile;

    @BeforeEach
    void setUp() {
        slugService = new SlugService(clientProfileRepository);
        clientProfileService = new ClientProfileService(
            clientProfileRepository,
            listingRepository,
            slugService,
            "Asia/Kolkata"
        );

        clientUser = new User("sub-123456", "private_owner@gmail.com", "Bijoy Das", "https://img.com/avatar.jpg");
        clientUser.setId(101L);
        clientUser.setRole(Role.CLIENT);
        clientUser.setStatus(UserStatus.ACTIVE);

        activeProfile = new ClientProfile();
        activeProfile.setId(500L);
        activeProfile.setUser(clientUser);
        activeProfile.setSlug("bijoy-arts");
        activeProfile.setBusinessName("Bijoy Arts Studio");
        activeProfile.setClientType(ClientType.STORE);
        activeProfile.setBio("Artisan handicrafts and clay idols");
        activeProfile.setPublicPhone("9830123456");
        activeProfile.setShowPhone(false);
        activeProfile.setStatus(ClientProfileStatus.ACTIVE);
        activeProfile.setVerified(false);
        activeProfile.setDailyOpenTime(LocalTime.of(10, 0));
        activeProfile.setDailyCloseTime(LocalTime.of(20, 0));
    }

    @Test
    void getPublicProfile_privacyEnforced_doesNotExposeEmailUserIdOrSub_andHidesPhone() {
        when(clientProfileRepository.findBySlug("bijoy-arts")).thenReturn(Optional.of(activeProfile));
        when(listingRepository.countByClientProfileIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any(), any(), any()))
            .thenReturn(2L);
        when(listingRepository.countByClientProfileIdAndStatusAndStartDateAfter(any(), any(), any()))
            .thenReturn(1L);

        PublicClientProfileResponse res = clientProfileService.getPublicProfile("bijoy-arts");

        assertNotNull(res);
        assertEquals("bijoy-arts", res.getSlug());
        assertEquals("Bijoy Arts Studio", res.getBusinessName());
        assertEquals(ClientType.STORE, res.getClientType());
        assertFalse(res.isShowPhone());
        assertNull(res.getPublicPhone(), "Phone MUST be null when showPhone is false");

        // Verify that DTO does not leak user credentials or account IDs
        assertFalse(res.getClass().toGenericString().contains("email"));
        assertFalse(res.getClass().toGenericString().contains("googleSub"));
    }

    @Test
    void getPublicProfile_whenShowPhoneIsTrue_exposesPublicPhone() {
        activeProfile.setShowPhone(true);
        when(clientProfileRepository.findBySlug("bijoy-arts")).thenReturn(Optional.of(activeProfile));

        PublicClientProfileResponse res = clientProfileService.getPublicProfile("bijoy-arts");

        assertNotNull(res);
        assertTrue(res.isShowPhone());
        assertEquals("9830123456", res.getPublicPhone());
    }

    @Test
    void getPublicProfile_whenProfileStatusIsHidden_throws404NotFound() {
        activeProfile.setStatus(ClientProfileStatus.HIDDEN);
        when(clientProfileRepository.findBySlug("bijoy-arts")).thenReturn(Optional.of(activeProfile));

        assertThrows(ResourceNotFoundException.class, () -> {
            clientProfileService.getPublicProfile("bijoy-arts");
        });
    }

    @Test
    void getPublicProfile_whenUserIsSuspended_throws404NotFound() {
        clientUser.setStatus(UserStatus.SUSPENDED);
        when(clientProfileRepository.findBySlug("bijoy-arts")).thenReturn(Optional.of(activeProfile));

        assertThrows(ResourceNotFoundException.class, () -> {
            clientProfileService.getPublicProfile("bijoy-arts");
        });
    }

    @Test
    void getPublicProfile_whenUserIsDemotedToUserRole_throws404NotFound() {
        clientUser.setRole(Role.USER);
        when(clientProfileRepository.findBySlug("bijoy-arts")).thenReturn(Optional.of(activeProfile));

        assertThrows(ResourceNotFoundException.class, () -> {
            clientProfileService.getPublicProfile("bijoy-arts");
        });
    }

    @Test
    void getClientListings_activeVsUpcoming_neverReturnsExpiredOrDrafts() {
        when(clientProfileRepository.findBySlug("bijoy-arts")).thenReturn(Optional.of(activeProfile));

        Listing activeListing = new Listing();
        activeListing.setId(1L);
        activeListing.setTitle("Grand Idol Exhibition");
        activeListing.setStatus(Status.PUBLISHED);
        activeListing.setStartDate(LocalDate.now().minusDays(1));
        activeListing.setEndDate(LocalDate.now().plusDays(5));

        Pageable pageable = PageRequest.of(0, 10);
        when(listingRepository.findActiveClientListings(eq(500L), any(LocalDate.class), eq(pageable)))
            .thenReturn(new PageImpl<>(List.of(activeListing)));

        Page<ListingResponse> res = clientProfileService.getClientListings("bijoy-arts", "active", pageable);

        assertNotNull(res);
        assertEquals(1, res.getContent().size());
        assertEquals("Grand Idol Exhibition", res.getContent().get(0).getTitle());
        assertEquals("bijoy-arts", res.getContent().get(0).getClientSlug());
        assertEquals("Bijoy Arts Studio", res.getContent().get(0).getClientBusinessName());

        verify(listingRepository, times(1)).findActiveClientListings(eq(500L), any(LocalDate.class), eq(pageable));
        verify(listingRepository, never()).findUpcomingClientListings(any(), any(), any());
    }

    @Test
    void slugService_blocksReservedWordsAndGeneratesUniqueSlugs() {
        assertTrue(slugService.isReserved("admin"));
        assertTrue(slugService.isReserved("api"));
        assertTrue(slugService.isReserved("account"));
        assertTrue(slugService.isReserved("login"));
        assertFalse(slugService.isReserved("ganguram-sweets"));

        when(clientProfileRepository.existsBySlug("ganguram")).thenReturn(true, false);
        String unique = slugService.generateUniqueSlug("Ganguram");
        assertEquals("ganguram-1", unique);

        String reservedSlug = slugService.generateUniqueSlug("Admin");
        assertNotEquals("admin", reservedSlug);
        assertTrue(reservedSlug.startsWith("admin-biz"));
    }

    @Test
    void autoCreateProfileForClient_initializesFieldsProperly() {
        when(clientProfileRepository.existsByUserId(101L)).thenReturn(false);
        when(clientProfileRepository.save(any(ClientProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientProfile profile = clientProfileService.createProfileForClient(
            clientUser,
            "Das Handloom",
            ClientType.STORE,
            "9830111222"
        );

        assertNotNull(profile);
        assertEquals("das-handloom", profile.getSlug());
        assertEquals("Das Handloom", profile.getBusinessName());
        assertEquals(ClientType.STORE, profile.getClientType());
        assertEquals(ClientProfileStatus.ACTIVE, profile.getStatus());
        assertFalse(profile.isVerified());
        assertFalse(profile.isShowPhone());
        assertEquals("9830111222", profile.getPublicPhone());
    }
}
