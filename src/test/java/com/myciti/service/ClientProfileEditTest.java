package com.myciti.service;

import com.myciti.dto.ClientProfileResponse;
import com.myciti.dto.UpdateClientProfileRequest;
import com.myciti.model.*;
import com.myciti.repository.ClientProfileRepository;
import com.myciti.repository.ListingRepository;
import com.myciti.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClientProfileEditTest {

    @Mock
    private ClientProfileRepository clientProfileRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    private SlugService slugService;
    private ClientProfileService clientProfileService;

    private User clientUser;
    private User regularUser;
    private ClientProfile existingProfile;

    @BeforeEach
    void setUp() {
        slugService = new SlugService(clientProfileRepository);
        clientProfileService = new ClientProfileService(
            clientProfileRepository,
            listingRepository,
            userRepository,
            slugService,
            storageService,
            "Asia/Kolkata",
            6.00, 37.50, 68.00, 97.50
        );

        clientUser = new User("sub-100", "client@test.com", "Bijoy Das", "https://img.com/avatar.jpg");
        clientUser.setId(10L);
        clientUser.setRole(Role.CLIENT);
        clientUser.setStatus(UserStatus.ACTIVE);

        regularUser = new User("sub-200", "user@test.com", "Regular User", "https://img.com/user.jpg");
        regularUser.setId(20L);
        regularUser.setRole(Role.USER);
        regularUser.setStatus(UserStatus.ACTIVE);

        existingProfile = new ClientProfile();
        existingProfile.setId(50L);
        existingProfile.setUser(clientUser);
        existingProfile.setSlug("bijoy-sweets");
        existingProfile.setBusinessName("Bijoy Sweets");
        existingProfile.setClientType(ClientType.STORE);
        existingProfile.setPublicPhone("9876543210");
        existingProfile.setShowPhone(false);
        existingProfile.setVerified(false);
        existingProfile.setStatus(ClientProfileStatus.ACTIVE);
        existingProfile.setCreatedAt(Instant.now());
        existingProfile.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("GET /api/me/client-profile: Client receives full profile including hidden publicPhone")
    void getMyProfile_success_returnsPrivateFields() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));
        when(clientProfileRepository.findByUserId(10L)).thenReturn(Optional.of(existingProfile));

        ClientProfileResponse res = clientProfileService.getMyClientProfile(10L);

        assertNotNull(res);
        assertEquals("bijoy-sweets", res.getSlug());
        assertEquals("Bijoy Sweets", res.getBusinessName());
        assertEquals(ClientType.STORE, res.getClientType());
        // In client's own view, publicPhone is visible even though showPhone is false
        assertEquals("9876543210", res.getPublicPhone());
        assertFalse(res.isShowPhone());
    }

    @Test
    @DisplayName("GET /api/me/client-profile: Non-client user is denied access with 403")
    void getMyProfile_nonClient_throwsAccessDenied() {
        when(userRepository.findById(20L)).thenReturn(Optional.of(regularUser));

        assertThrows(AccessDeniedException.class, () -> {
            clientProfileService.getMyClientProfile(20L);
        });
    }

    @Test
    @DisplayName("GET /api/me/client-profile: Auto-creates profile if missing for approved client")
    void getMyProfile_missingProfile_autoCreates() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));
        when(clientProfileRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(clientProfileRepository.existsByUserId(10L)).thenReturn(false);
        when(clientProfileRepository.existsBySlug("bijoy-das")).thenReturn(false);
        when(clientProfileRepository.save(any(ClientProfile.class))).thenAnswer(i -> {
            ClientProfile cp = i.getArgument(0);
            cp.setId(99L);
            return cp;
        });

        ClientProfileResponse res = clientProfileService.getMyClientProfile(10L);

        assertNotNull(res);
        assertEquals("bijoy-das", res.getSlug());
        verify(clientProfileRepository, times(1)).save(any(ClientProfile.class));
    }

    @Test
    @DisplayName("PUT /api/me/client-profile: Strips HTML and validates phone and coords")
    void updateMyProfile_stripsHtml_andValidates() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));
        when(clientProfileRepository.findByUserId(10L)).thenReturn(Optional.of(existingProfile));
        when(clientProfileRepository.save(any(ClientProfile.class))).thenAnswer(i -> i.getArgument(0));

        UpdateClientProfileRequest req = new UpdateClientProfileRequest();
        req.setBusinessName("<b>Sweet Shop Deluxe</b>");
        req.setBio("<script>alert('x')</script>Traditional sweets and savories since 1985.");
        req.setAddress("<div>123 College St, Kolkata</div>");
        req.setLandmark("Near <span>Metro Gate 2</span>");
        req.setLatitude(22.5726);
        req.setLongitude(88.3639);
        req.setPublicPhone("+91-9830012345");
        req.setShowPhone(true);
        req.setWhatsappNumber("+91 9830099999");
        req.setDailyOpenTime(LocalTime.of(9, 0));
        req.setDailyCloseTime(LocalTime.of(21, 30));

        ClientProfileResponse updated = clientProfileService.updateMyClientProfile(10L, req);

        assertNotNull(updated);
        assertEquals("Sweet Shop Deluxe", updated.getBusinessName());
        assertEquals("Sweet Shop Deluxe", clientUser.getBusinessName());
        assertEquals("alert('x')Traditional sweets and savories since 1985.", updated.getBio());
        assertEquals("123 College St, Kolkata", updated.getAddress());
        assertEquals("Near Metro Gate 2", updated.getLandmark());
        assertEquals(22.5726, updated.getLatitude());
        assertEquals(88.3639, updated.getLongitude());
        assertEquals("9830012345", updated.getPublicPhone());
        assertTrue(updated.isShowPhone());
        assertEquals("9830099999", updated.getWhatsappNumber());
        assertEquals(LocalTime.of(9, 0), updated.getDailyOpenTime());
        assertEquals(LocalTime.of(21, 30), updated.getDailyCloseTime());

        // Invariant: slug must remain immutable
        assertEquals("bijoy-sweets", updated.getSlug());
        assertFalse(updated.isVerified());
    }

    @Test
    @DisplayName("PUT /api/me/client-profile: Rejects coordinates outside city bounding box")
    void updateMyProfile_coordinatesOutsideBbox_throwsIllegalArgument() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));
        when(clientProfileRepository.findByUserId(10L)).thenReturn(Optional.of(existingProfile));

        UpdateClientProfileRequest req = new UpdateClientProfileRequest();
        req.setLatitude(51.5074); // London latitude (outside India bbox 6.00 - 37.50)
        req.setLongitude(88.3639);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            clientProfileService.updateMyClientProfile(10L, req);
        });
        assertTrue(ex.getMessage().contains("outside the supported boundary"));
    }

    @Test
    @DisplayName("PUT /api/me/client-profile: Rejects single coordinate provided without the other")
    void updateMyProfile_singleCoordinate_throwsIllegalArgument() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));
        when(clientProfileRepository.findByUserId(10L)).thenReturn(Optional.of(existingProfile));

        UpdateClientProfileRequest req = new UpdateClientProfileRequest();
        req.setLatitude(22.5726);
        req.setLongitude(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            clientProfileService.updateMyClientProfile(10L, req);
        });
        assertTrue(ex.getMessage().contains("Both latitude and longitude must be provided together"));
    }

    @Test
    @DisplayName("PUT /api/me/client-profile: Rejects invalid Indian phone number format")
    void updateMyProfile_invalidPhone_throwsIllegalArgument() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));
        when(clientProfileRepository.findByUserId(10L)).thenReturn(Optional.of(existingProfile));

        UpdateClientProfileRequest req = new UpdateClientProfileRequest();
        req.setPublicPhone("1234567890"); // Starts with 1, invalid Indian mobile (must be 6-9)

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            clientProfileService.updateMyClientProfile(10L, req);
        });
        assertTrue(ex.getMessage().contains("Public phone must be a valid 10-digit Indian mobile number"));
    }

    @Test
    @DisplayName("POST /api/me/client-profile/logo: Uploads logo and deletes replaced image")
    void updateLogo_success_deletesOldImage() throws IOException {
        existingProfile.setLogoUrl("https://cloudinary.com/myciti/old_logo.jpg");
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));
        when(clientProfileRepository.findByUserId(10L)).thenReturn(Optional.of(existingProfile));
        when(storageService.storeClientLogo(any())).thenReturn("https://cloudinary.com/myciti/new_logo.jpg");

        MockMultipartFile file = new MockMultipartFile("file", "logo.jpg", "image/jpeg", new byte[]{ (byte)0xFF, (byte)0xD8, (byte)0xFF });

        String newUrl = clientProfileService.updateLogo(10L, file);

        assertEquals("https://cloudinary.com/myciti/new_logo.jpg", newUrl);
        assertEquals("https://cloudinary.com/myciti/new_logo.jpg", existingProfile.getLogoUrl());
        verify(storageService, times(1)).deleteImage("https://cloudinary.com/myciti/old_logo.jpg");
    }

    @Test
    @DisplayName("POST /api/me/client-profile/cover: Uploads cover and deletes replaced image")
    void updateCover_success_deletesOldImage() throws IOException {
        existingProfile.setCoverUrl("https://cloudinary.com/myciti/old_cover.jpg");
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));
        when(clientProfileRepository.findByUserId(10L)).thenReturn(Optional.of(existingProfile));
        when(storageService.storeClientCover(any())).thenReturn("https://cloudinary.com/myciti/new_cover.jpg");

        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", new byte[]{ (byte)0x89, 0x50, 0x4E, 0x47 });

        String newUrl = clientProfileService.updateCover(10L, file);

        assertEquals("https://cloudinary.com/myciti/new_cover.jpg", newUrl);
        assertEquals("https://cloudinary.com/myciti/new_cover.jpg", existingProfile.getCoverUrl());
        verify(storageService, times(1)).deleteImage("https://cloudinary.com/myciti/old_cover.jpg");
    }
}
