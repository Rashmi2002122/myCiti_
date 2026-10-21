package com.myciti.controller;

import com.myciti.dto.ListingResponse;
import com.myciti.dto.PublicClientProfileResponse;
import com.myciti.service.ClientProfileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
public class PublicClientController {

    private final ClientProfileService clientProfileService;

    public PublicClientController(ClientProfileService clientProfileService) {
        this.clientProfileService = clientProfileService;
    }

    /**
     * Public endpoint: GET /api/clients/{slug}
     * Returns public client profile without authentication.
     * Enforces strict privacy rules (no emails, user IDs, hidden phone unless showPhone = true).
     */
    @GetMapping("/{slug}")
    public ResponseEntity<PublicClientProfileResponse> getPublicProfile(@PathVariable String slug) {
        return ResponseEntity.ok(clientProfileService.getPublicProfile(slug));
    }

    /**
     * Public endpoint: GET /api/clients/{slug}/listings?tab=active|upcoming&page=&size=
     * Returns only PUBLISHED, non-expired listings of that client.
     */
    @GetMapping("/{slug}/listings")
    public ResponseEntity<Page<ListingResponse>> getClientListings(
        @PathVariable String slug,
        @RequestParam(defaultValue = "active") String tab,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(clientProfileService.getClientListings(slug, tab, pageable));
    }
}
