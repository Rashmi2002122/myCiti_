package com.myciti.controller;

import com.myciti.dto.AdCampaignResponse;
import com.myciti.dto.AdReportDto;
import com.myciti.model.CampaignStatus;
import com.myciti.service.AdCampaignService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ads")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAdController {

    private final AdCampaignService adCampaignService;

    public AdminAdController(AdCampaignService adCampaignService) {
        this.adCampaignService = adCampaignService;
    }

    @GetMapping
    public ResponseEntity<List<AdCampaignResponse>> getAllCampaigns(
        @RequestParam(required = false) CampaignStatus status
    ) {
        return ResponseEntity.ok(adCampaignService.getAllCampaignsAdmin(status));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<AdCampaignResponse> approveCampaign(@PathVariable Long id) {
        return ResponseEntity.ok(adCampaignService.approveCampaignAdmin(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<AdCampaignResponse> rejectCampaign(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Does not meet advertising guidelines";
        return ResponseEntity.ok(adCampaignService.rejectCampaignAdmin(id, reason));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable Long id) {
        adCampaignService.deleteCampaignAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reports")
    public ResponseEntity<List<AdReportDto>> getReports() {
        return ResponseEntity.ok(adCampaignService.getAllReportsAdmin());
    }
}
