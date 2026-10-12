package com.myciti.controller;

import com.myciti.dto.SubmissionRequest;
import com.myciti.dto.SubmissionResponse;
import com.myciti.model.SubmissionStatus;
import com.myciti.model.User;
import com.myciti.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /**
     * POST /api/submissions
     * Create a new advertisement submission (status PENDING).
     */
    @PostMapping
    public ResponseEntity<SubmissionResponse> createSubmission(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody SubmissionRequest request
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        SubmissionResponse response = submissionService.createSubmission(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/submissions?status=
     * List current user's submissions, optionally filtered by status.
     */
    @GetMapping
    public ResponseEntity<List<SubmissionResponse>> getMySubmissions(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) SubmissionStatus status
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<SubmissionResponse> list = submissionService.getUserSubmissions(currentUser.getId(), status);
        return ResponseEntity.ok(list);
    }

    /**
     * GET /api/submissions/{id}
     * Get details of a single submission owned by current user.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SubmissionResponse> getSubmissionById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        SubmissionResponse response = submissionService.getSubmissionById(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/submissions/{id}
     * Update an existing submission.
     * If PENDING or DECLINED, updates in-place and sets to PENDING.
     * If APPROVED, creates a new PENDING revision pointing to the live listing.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SubmissionResponse> updateSubmission(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody SubmissionRequest request
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        SubmissionResponse response = submissionService.updateSubmission(id, currentUser.getId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/submissions/{id}
     * Delete a PENDING or DECLINED submission.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubmission(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        submissionService.deleteSubmission(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/submissions/{id}/unpublish
     * Unpublishes an APPROVED listing.
     */
    @PostMapping("/{id}/unpublish")
    public ResponseEntity<SubmissionResponse> unpublishSubmission(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        SubmissionResponse response = submissionService.unpublishSubmission(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/submissions/{id}/images
     * Upload up to 5 photos for a submission (max 5 MB each, re-encoded & EXIF stripped).
     */
    @PostMapping("/{id}/images")
    public ResponseEntity<List<com.myciti.dto.SubmissionImageDto>> uploadImages(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @RequestParam(value = "files", required = false) List<org.springframework.web.multipart.MultipartFile> files,
            @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file
    ) throws java.io.IOException {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<org.springframework.web.multipart.MultipartFile> uploadList = new java.util.ArrayList<>();
        if (files != null && !files.isEmpty()) {
            uploadList.addAll(files);
        } else if (file != null && !file.isEmpty()) {
            uploadList.add(file);
        }

        if (uploadList.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<com.myciti.dto.SubmissionImageDto> images = submissionService.addImagesToSubmission(id, currentUser.getId(), uploadList);
        return ResponseEntity.status(HttpStatus.CREATED).body(images);
    }

    /**
     * DELETE /api/submissions/{id}/images/{imageId}
     * Remove an uploaded photo from a submission.
     */
    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @PathVariable Long imageId
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        submissionService.deleteImageFromSubmission(id, imageId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
