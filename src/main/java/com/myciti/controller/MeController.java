package com.myciti.controller;

import com.myciti.dto.ListingResponse;
import com.myciti.dto.UpdateLocationRequest;
import com.myciti.dto.UpdateProfileRequest;
import com.myciti.dto.UserDto;
import com.myciti.model.User;
import com.myciti.service.UserService;
import com.myciti.service.UserSavedListingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;
    private final UserSavedListingService userSavedListingService;

    public MeController(UserService userService, UserSavedListingService userSavedListingService) {
        this.userService = userService;
        this.userSavedListingService = userSavedListingService;
    }

    /**
     * GET /api/me
     * Returns current user's profile, role, profileComplete, clientType, and counts.
     */
    @GetMapping
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDto profile = userService.getProfile(currentUser.getId());
        return ResponseEntity.ok(profile);
    }

    /**
     * PUT /api/me
     * Updates basic profile: name, phone, or client business profile if client.
     */
    @PutMapping
    public ResponseEntity<UserDto> updateProfile(
        @AuthenticationPrincipal User currentUser,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDto updated = userService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(updated);
    }

    /**
     * PUT /api/me/location
     * Updates user's coarse location and city for geo-targeted notifications.
     */
    @PutMapping("/location")
    public ResponseEntity<UserDto> updateLocation(
        @AuthenticationPrincipal User currentUser,
        @Valid @RequestBody UpdateLocationRequest request
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDto updated = userService.updateLocation(currentUser.getId(), request);
        return ResponseEntity.ok(updated);
    }

    /**
     * GET /api/me/saved
     * List listings saved / bookmarked by the logged-in user.
     */
    @GetMapping("/saved")
    public ResponseEntity<List<ListingResponse>> getSavedListings(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<ListingResponse> saved = userSavedListingService.getSavedListings(currentUser.getId());
        return ResponseEntity.ok(saved);
    }

    /**
     * POST /api/me/saved/{listingId}
     * Save a listing.
     */
    @PostMapping("/saved/{listingId}")
    public ResponseEntity<Map<String, Object>> saveListing(
        @AuthenticationPrincipal User currentUser,
        @PathVariable Long listingId
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        boolean saved = userSavedListingService.saveListing(currentUser.getId(), listingId);
        return ResponseEntity.ok(Map.of("saved", saved, "listingId", listingId));
    }

    /**
     * DELETE /api/me/saved/{listingId}
     * Unsave a listing.
     */
    @DeleteMapping("/saved/{listingId}")
    public ResponseEntity<Map<String, Object>> unsaveListing(
        @AuthenticationPrincipal User currentUser,
        @PathVariable Long listingId
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userSavedListingService.unsaveListing(currentUser.getId(), listingId);
        return ResponseEntity.ok(Map.of("saved", false, "listingId", listingId));
    }

    /**
     * GET /api/me/saved/{listingId}
     * Check if a listing is saved by current user.
     */
    @GetMapping("/saved/{listingId}")
    public ResponseEntity<Map<String, Object>> isListingSaved(
        @AuthenticationPrincipal User currentUser,
        @PathVariable Long listingId
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        boolean saved = userSavedListingService.isSaved(currentUser.getId(), listingId);
        return ResponseEntity.ok(Map.of("saved", saved, "listingId", listingId));
    }
}
