package com.myciti.service;

import com.myciti.dto.AudienceEstimateResponse;
import com.myciti.model.TargetType;
import com.myciti.model.User;
import com.myciti.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class GeoTargetingService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private final UserRepository userRepository;

    public GeoTargetingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Exact spherical Haversine formula in kilometers.
     */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Find target active users matching the targeting criteria.
     * Note: Coordinates are coarse (~1km) and NEVER logged.
     */
    @Transactional(readOnly = true)
    public List<User> findTargetUsers(TargetType targetType, Double centerLat, Double centerLng, Double radiusKm, String targetCity) {
        if (targetType == null) {
            targetType = TargetType.RADIUS;
        }

        switch (targetType) {
            case RADIUS -> {
                if (centerLat == null || centerLng == null || radiusKm == null || radiusKm <= 0) {
                    return Collections.emptyList();
                }
                // Bounding-box prefilter:
                // 1 deg latitude ≈ 111.0 km
                double deltaLat = radiusKm / 111.0;
                double cosLat = Math.cos(Math.toRadians(centerLat));
                double deltaLng = radiusKm / (111.0 * (Math.abs(cosLat) > 0.001 ? Math.abs(cosLat) : 1.0));

                double minLat = centerLat - deltaLat;
                double maxLat = centerLat + deltaLat;
                double minLng = centerLng - deltaLng;
                double maxLng = centerLng + deltaLng;

                List<User> candidates = userRepository.findActiveUsersInBoundingBox(minLat, maxLat, minLng, maxLng);
                return candidates.stream()
                    .filter(u -> u.getCoarseLat() != null && u.getCoarseLng() != null)
                    .filter(u -> haversineKm(centerLat, centerLng, u.getCoarseLat(), u.getCoarseLng()) <= radiusKm)
                    .toList();
            }
            case CITY -> {
                if (targetCity == null || targetCity.isBlank()) {
                    return Collections.emptyList();
                }
                return userRepository.findActiveUsersByAreaName(targetCity.trim());
            }
            case BROADCAST -> {
                return userRepository.findAllActiveUsers();
            }
            default -> {
                return Collections.emptyList();
            }
        }
    }

    /**
     * Estimate audience size privacy-safely.
     * Never returns exact counts or lists of users.
     * Masks count if under 10 users.
     */
    @Transactional(readOnly = true)
    public AudienceEstimateResponse estimateAudience(TargetType targetType, Double centerLat, Double centerLng, Double radiusKm, String targetCity) {
        List<User> matches = findTargetUsers(targetType, centerLat, centerLng, radiusKm, targetCity);
        int count = matches.size();

        if (count < 10) {
            return new AudienceEstimateResponse(targetType, radiusKm, targetCity, "< 10 users in this area", false);
        } else if (count < 50) {
            return new AudienceEstimateResponse(targetType, radiusKm, targetCity, "10 – 50 people", true);
        } else if (count < 200) {
            return new AudienceEstimateResponse(targetType, radiusKm, targetCity, "50 – 200 people", true);
        } else {
            return new AudienceEstimateResponse(targetType, radiusKm, targetCity, "200+ people", true);
        }
    }
}
