package com.myciti.controller;

import com.myciti.dto.AdminSubmissionsResponse;
import com.myciti.dto.DeclineRequest;
import com.myciti.dto.SubmissionResponse;
import com.myciti.model.SubmissionStatus;
import com.myciti.model.User;
import com.myciti.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/submissions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSubmissionController {

    private final SubmissionService submissionService;

    public AdminSubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /**
     * GET /api/admin/submissions?status=PENDING
     * List submissions for owner review (oldest first, with status counts).
     */
    @GetMapping
    public ResponseEntity<AdminSubmissionsResponse> getSubmissions(
            @RequestParam(required = false) SubmissionStatus status
    ) {
        AdminSubmissionsResponse response = submissionService.getAdminSubmissions(status);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/submissions/{id}
     * Get single submission full review details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SubmissionResponse> getSubmissionById(@PathVariable Long id) {
        SubmissionResponse response = submissionService.getAdminSubmissionById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/admin/submissions/{id}/approve
     * Approve submission and create/update live Listing (transactional & idempotent).
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<SubmissionResponse> approveSubmission(
            @PathVariable Long id,
            @AuthenticationPrincipal User adminUser
    ) {
        SubmissionResponse response = submissionService.approveSubmission(id, adminUser);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/admin/submissions/{id}/decline
     * Decline submission with required reason (minimum 10 characters).
     */
    @PostMapping("/{id}/decline")
    public ResponseEntity<SubmissionResponse> declineSubmission(
            @PathVariable Long id,
            @Valid @RequestBody DeclineRequest request,
            @AuthenticationPrincipal User adminUser
    ) {
        SubmissionResponse response = submissionService.declineSubmission(id, request.getReason(), adminUser);
        return ResponseEntity.ok(response);
    }
}
