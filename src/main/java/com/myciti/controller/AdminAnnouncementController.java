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
}
