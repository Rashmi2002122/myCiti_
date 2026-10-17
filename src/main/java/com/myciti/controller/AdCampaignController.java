package com.myciti.controller;

import com.myciti.dto.*;
import com.myciti.model.Role;
import com.myciti.model.TargetType;
import com.myciti.model.User;
import com.myciti.service.AdCampaignService;
import com.myciti.service.GeoTargetingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ads")
public class AdCampaignController {

    private final AdCampaignService adCampaignService;
    private final GeoTargetingService geoTargetingService;

    public AdCampaignController(AdCampaignService adCampaignService, GeoTargetingService geoTargetingService) {
        this.adCampaignService = adCampaignService;
        this.geoTargetingService = geoTargetingService;
    }

    /**
     * Public endpoint: retrieve active sponsored ads for feed & map.
     */
    @GetMapping("/active")
    public ResponseEntity<List<ActiveAdDto>> getActiveAds(
        @RequestParam(required = false) Double lat,
        @RequestParam(required = false) Double lng,
        @RequestParam(required = false) String city
    ) {
        return ResponseEntity.ok(adCampaignService.getActiveAds(lat, lng, city));
    }

    /**
     * Public: record impression.
     */
    @PostMapping("/campaigns/{id}/impressions")
    public ResponseEntity<Void> recordImpressions(
        @PathVariable Long id,
        @RequestParam(defaultValue = "1") long count
    ) {
        adCampaignService.recordImpressions(id, count);
        return ResponseEntity.noContent().build();
    }

    /**
     * Public: record click.
     */
    @PostMapping("/campaigns/{id}/click")
    public ResponseEntity<Void> recordClick(@PathVariable Long id) {
        adCampaignService.recordClick(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Public / Authenticated: report an ad.
     */
    @PostMapping("/campaigns/{id}/report")
    public ResponseEntity<Map<String, String>> reportAd(
        @AuthenticationPrincipal User user,
        @PathVariable Long id,
        @Valid @RequestBody AdReportRequest req
    ) {
        adCampaignService.reportAd(user != null ? user.getId() : null, id, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Ad reported successfully. Thank you for keeping myCiti safe."));
    }

    /**
     * Client / Admin: create campaign.
     */
    @PostMapping("/campaigns")
    public ResponseEntity<AdCampaignResponse> createCampaign(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody AdCampaignRequest req
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        if (user.getRole() != Role.CLIENT && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only CLIENT and ADMIN accounts can create ad campaigns");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(adCampaignService.createCampaign(user.getId(), req));
    }

    /**
     * Client / Admin: get my campaigns.
     */
    @GetMapping("/my-campaigns")
    public ResponseEntity<List<AdCampaignResponse>> getMyCampaigns(
        @AuthenticationPrincipal User user
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        if (user.getRole() != Role.CLIENT && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only CLIENT and ADMIN accounts can view campaign dashboards");
        }
        return ResponseEntity.ok(adCampaignService.getClientCampaigns(user.getId()));
    }

    /**
     * Client / Admin: audience size estimation.
     * Masked if < 10 users to protect privacy.
     */
    @GetMapping("/audience-estimate")
    public ResponseEntity<AudienceEstimateResponse> estimateAudience(
        @AuthenticationPrincipal User user,
        @RequestParam(defaultValue = "RADIUS") TargetType targetType,
        @RequestParam(required = false) Double lat,
        @RequestParam(required = false) Double lng,
        @RequestParam(defaultValue = "5.0") Double radiusKm,
        @RequestParam(required = false) String city
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        if (user.getRole() != Role.CLIENT && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only CLIENT and ADMIN accounts can query audience estimates");
        }
        return ResponseEntity.ok(geoTargetingService.estimateAudience(targetType, lat, lng, radiusKm, city));
    }

    /**
     * Client / Admin: pause campaign.
     */
    @PostMapping("/campaigns/{id}/pause")
    public ResponseEntity<AdCampaignResponse> pauseCampaign(
        @AuthenticationPrincipal User user,
        @PathVariable Long id
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(adCampaignService.pauseCampaign(user.getId(), id));
    }

    /**
     * Client / Admin: resume campaign.
     */
    @PostMapping("/campaigns/{id}/resume")
    public ResponseEntity<AdCampaignResponse> resumeCampaign(
        @AuthenticationPrincipal User user,
        @PathVariable Long id
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(adCampaignService.resumeCampaign(user.getId(), id));
    }

    /**
     * Client / Admin: delete campaign.
     */
    @DeleteMapping("/campaigns/{id}")
    public ResponseEntity<Void> deleteCampaign(
        @AuthenticationPrincipal User user,
        @PathVariable Long id
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        adCampaignService.deleteCampaign(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
