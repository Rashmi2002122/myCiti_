package com.myciti.controller;

import com.myciti.dto.AnnouncementResponse;
import com.myciti.service.AnnouncementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/announcements")
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    public AdminAnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public ResponseEntity<List<AnnouncementResponse>> getAllAnnouncements() {
        return ResponseEntity.ok(announcementService.getAllAnnouncementsAdmin());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<AnnouncementResponse> approveAnnouncement(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.approveAnnouncementAdmin(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<AnnouncementResponse> rejectAnnouncement(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Does not meet community guidelines";
        return ResponseEntity.ok(announcementService.rejectAnnouncementAdmin(id, reason));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncementAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/broadcast")
    public ResponseEntity<AnnouncementResponse> broadcastAnnouncement(
        @org.springframework.security.core.annotation.AuthenticationPrincipal com.myciti.model.User user,
        @jakarta.validation.Valid @RequestBody com.myciti.dto.AnnouncementRequest req
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
            .body(announcementService.createAnnouncement(user.getId(), req));
    }
}
