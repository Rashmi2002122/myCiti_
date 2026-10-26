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

    /**
     * Public / Authenticated endpoint: POST /api/clients/{slug}/report
     * Report a client business profile for fraudulent or abusive behavior.
     */
    @PostMapping("/{slug}/report")
    public ResponseEntity<java.util.Map<String, String>> reportProfile(
        @PathVariable String slug,
        @org.springframework.security.core.annotation.AuthenticationPrincipal com.myciti.model.User user,
        @jakarta.validation.Valid @RequestBody com.myciti.dto.ProfileReportRequest req
    ) {
        Long reporterId = (user != null) ? user.getId() : null;
        clientProfileService.reportProfile(slug, reporterId, req);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
            .body(java.util.Map.of("message", "Profile report submitted successfully. Thank you for keeping myCiti safe."));
    }
}