package com.myciti.controller;

import com.myciti.dto.ImageResponse;
import com.myciti.dto.ListingRequest;
import com.myciti.dto.ListingResponse;
import com.myciti.service.ListingService;
import com.myciti.service.StorageService;
import com.myciti.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ListingService listingService;
    private final StorageService storageService;
    private final UserService userService;
    private final com.myciti.service.ClientProfileService clientProfileService;

    public AdminController(
        ListingService listingService,
        StorageService storageService,
        UserService userService,
        com.myciti.service.ClientProfileService clientProfileService
    ) {
        this.listingService = listingService;
        this.storageService = storageService;
        this.userService = userService;
        this.clientProfileService = clientProfileService;
    }

    /**
     * GET /api/admin/listings
     */
    @GetMapping("/listings")
    public ResponseEntity<List<ListingResponse>> getAllListings() {
        return ResponseEntity.ok(listingService.getAllListingsForAdmin());
    }

    /**
     * POST /api/admin/listings
     */
    @PostMapping("/listings")
    public ResponseEntity<ListingResponse> createListing(@Valid @RequestBody ListingRequest request) {
        ListingResponse created = listingService.createListing(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/admin/listings/{id}
     */
    @PutMapping("/listings/{id}")
    public ResponseEntity<ListingResponse> updateListing(
        @PathVariable Long id,
        @Valid @RequestBody ListingRequest request
    ) {
        ListingResponse updated = listingService.updateListing(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/admin/listings/{id}
     */
    @DeleteMapping("/listings/{id}")
    public ResponseEntity<Void> deleteListing(@PathVariable Long id) {
        listingService.deleteListing(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/admin/listings/{id}/images
     * Multipart upload, resizes to max 1200px, compresses to JPEG, saves to storage.
     */
    @PostMapping("/listings/{id}/images")
    public ResponseEntity<ImageResponse> uploadImage(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "sortOrder", required = false) Integer sortOrder
    ) throws IOException {
        String url = storageService.storeImage(file);
        ImageResponse response = listingService.addListingImage(id, url, sortOrder);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/admin/users?q=&page=&size=
     */
    @GetMapping("/users")
    public ResponseEntity<org.springframework.data.domain.Page<com.myciti.dto.UserDto>> getUsers(
        @RequestParam(required = false) String q,
        @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable
    ) {
        return ResponseEntity.ok(userService.searchUsers(q, pageable));
    }

    /**
     * PUT /api/admin/users/{id}/suspend
     */
    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<com.myciti.dto.UserDto> suspendUser(
        @PathVariable Long id,
        @org.springframework.security.core.annotation.AuthenticationPrincipal com.myciti.model.User adminUser
    ) {
        Long adminId = (adminUser != null) ? adminUser.getId() : 0L;
        return ResponseEntity.ok(userService.suspendUser(adminId, id));
    }

    /**
     * PUT /api/admin/users/{id}/unsuspend
     */
    @PutMapping("/users/{id}/unsuspend")
    public ResponseEntity<com.myciti.dto.UserDto> unsuspendUser(
        @PathVariable Long id,
        @org.springframework.security.core.annotation.AuthenticationPrincipal com.myciti.model.User adminUser
    ) {
        Long adminId = (adminUser != null) ? adminUser.getId() : 0L;
        return ResponseEntity.ok(userService.unsuspendUser(adminId, id));
    }

    /**
     * GET /api/admin/audit-logs
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<org.springframework.data.domain.Page<com.myciti.dto.AuditLogDto>> getAuditLogs(
        @org.springframework.data.web.PageableDefault(size = 50) org.springframework.data.domain.Pageable pageable
    ) {
        return ResponseEntity.ok(userService.getAuditLogs(pageable));
    }

    /**
     * GET /api/admin/client-profiles/reports
     */
    @GetMapping("/client-profiles/reports")
    public ResponseEntity<org.springframework.data.domain.Page<com.myciti.dto.ProfileReportDto>> getClientProfileReports(
        @RequestParam(required = false) com.myciti.model.ProfileReportStatus status,
        @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable
    ) {
        return ResponseEntity.ok(clientProfileService.getProfileReportsAdmin(status, pageable));
    }
}
