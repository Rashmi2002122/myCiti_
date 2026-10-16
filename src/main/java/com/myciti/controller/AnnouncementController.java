package com.myciti.controller;

import com.myciti.dto.AnnouncementRequest;
import com.myciti.dto.AnnouncementResponse;
import com.myciti.model.Role;
import com.myciti.model.User;
import com.myciti.service.AnnouncementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    /**
     * Client / Admin: create announcement.
     * Admin broadcasts are dispatched immediately.
     * Client announcements are submitted for review.
     */
    @PostMapping
    public ResponseEntity<AnnouncementResponse> createAnnouncement(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody AnnouncementRequest req
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        if (user.getRole() != Role.CLIENT && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only CLIENT and ADMIN accounts can create announcements");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(announcementService.createAnnouncement(user.getId(), req));
    }

    /**
     * Client / Admin: list my announcements.
     */
    @GetMapping("/my")
    public ResponseEntity<List<AnnouncementResponse>> getMyAnnouncements(
        @AuthenticationPrincipal User user
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        if (user.getRole() != Role.CLIENT && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only CLIENT and ADMIN accounts can view announcement history");
        }
        return ResponseEntity.ok(announcementService.getMyAnnouncements(user.getId()));
    }
}
