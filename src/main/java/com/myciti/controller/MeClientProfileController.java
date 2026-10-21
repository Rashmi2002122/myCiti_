package com.myciti.controller;

import com.myciti.dto.ClientProfileResponse;
import com.myciti.dto.UpdateClientProfileRequest;
import com.myciti.model.User;
import com.myciti.service.ClientProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/me/client-profile")
@PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
public class MeClientProfileController {

    private final ClientProfileService clientProfileService;

    public MeClientProfileController(ClientProfileService clientProfileService) {
        this.clientProfileService = clientProfileService;
    }

    /**
     * GET /api/me/client-profile
     * Fetch the authenticated client's own profile.
     */
    @GetMapping
    public ResponseEntity<ClientProfileResponse> getMyProfile(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ClientProfileResponse profile = clientProfileService.getMyClientProfile(currentUser.getId());
        return ResponseEntity.ok(profile);
    }

    /**
     * PUT /api/me/client-profile
     * Update the authenticated client's profile details.
     */
    @PutMapping
    public ResponseEntity<ClientProfileResponse> updateMyProfile(
        @AuthenticationPrincipal User currentUser,
        @Valid @RequestBody UpdateClientProfileRequest request
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ClientProfileResponse updated = clientProfileService.updateMyClientProfile(currentUser.getId(), request);
        return ResponseEntity.ok(updated);
    }

    /**
     * POST /api/me/client-profile/logo
     * Upload business logo (max 5 MB, resized to 400px, EXIF stripped).
     */
    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadLogo(
        @AuthenticationPrincipal User currentUser,
        @RequestParam("file") MultipartFile file
    ) throws IOException {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String url = clientProfileService.updateLogo(currentUser.getId(), file);
        return ResponseEntity.ok(Map.of("url", url, "logoUrl", url));
    }

    /**
     * POST /api/me/client-profile/cover
     * Upload business cover banner (max 5 MB, resized to 1200px wide, EXIF stripped).
     */
    @PostMapping(value = "/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadCover(
        @AuthenticationPrincipal User currentUser,
        @RequestParam("file") MultipartFile file
    ) throws IOException {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String url = clientProfileService.updateCover(currentUser.getId(), file);
        return ResponseEntity.ok(Map.of("url", url, "coverUrl", url));
    }
}
