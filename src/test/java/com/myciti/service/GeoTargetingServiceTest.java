package com.myciti.service;

import com.myciti.dto.AudienceEstimateResponse;
import com.myciti.model.TargetType;
import com.myciti.model.User;
import com.myciti.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GeoTargetingServiceTest {

    @Mock
    private UserRepository userRepository;

    private GeoTargetingService geoTargetingService;

    @BeforeEach
    void setUp() {
        geoTargetingService = new GeoTargetingService(userRepository);
    }

    @Test
    void haversineKm_calculatesAccurateDistance() {
        // Park Street (22.55, 88.35) to Salt Lake (22.58, 88.42) ≈ 7.9 km
        double d = GeoTargetingService.haversineKm(22.55, 88.35, 22.58, 88.42);
        assertTrue(d > 7.0 && d < 9.0, "Distance should be ~7.9km, but was: " + d);
    }

    @Test
    void findTargetUsers_radius_filtersCorrectly() {
        User nearUser = new User("sub-1", "near@test.com", "Near", null);
        nearUser.setId(1L);
        nearUser.setCoarseLat(22.56);
        nearUser.setCoarseLng(22.56);

        User farUser = new User("sub-2", "far@test.com", "Far", null);
        farUser.setId(2L);
        farUser.setCoarseLat(23.50);
        farUser.setCoarseLng(88.00);

        when(userRepository.findActiveUsersInBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
            .thenReturn(List.of(nearUser, farUser));

        List<User> matches = geoTargetingService.findTargetUsers(TargetType.RADIUS, 22.56, 22.56, 5.0, null);

        assertEquals(1, matches.size());
        assertEquals(1L, matches.get(0).getId());
    }

    @Test
    void estimateAudience_under10_masksExactCountForPrivacy() {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            User u = new User("sub-" + i, "u" + i + "@test.com", "U" + i, null);
            u.setCoarseLat(22.55);
            u.setCoarseLng(88.35);
            users.add(u);
        }

        when(userRepository.findActiveUsersInBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
            .thenReturn(users);

        AudienceEstimateResponse res = geoTargetingService.estimateAudience(TargetType.RADIUS, 22.55, 88.35, 5.0, null);

        assertNotNull(res);
        assertEquals("< 10 users in this area", res.estimatedRange());
        assertFalse(res.meetsPrivacyThreshold());
    }

    @Test
    void estimateAudience_between10And50_returnsRange() {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            User u = new User("sub-" + i, "u" + i + "@test.com", "U" + i, null);
            u.setCoarseLat(22.55);
            u.setCoarseLng(88.35);
            users.add(u);
        }

        when(userRepository.findActiveUsersInBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
            .thenReturn(users);

        AudienceEstimateResponse res = geoTargetingService.estimateAudience(TargetType.RADIUS, 22.55, 88.35, 5.0, null);

        assertNotNull(res);
        assertEquals("10 – 50 people", res.estimatedRange());
        assertTrue(res.meetsPrivacyThreshold());
    }
}
