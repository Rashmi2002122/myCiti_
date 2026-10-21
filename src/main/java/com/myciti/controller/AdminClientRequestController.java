package com.myciti.controller;

import com.myciti.dto.ClientRequestResponse;
import com.myciti.dto.DeclineRequest;
import com.myciti.model.ClientRequestStatus;
import com.myciti.model.User;
import com.myciti.service.ClientRequestService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/client-requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminClientRequestController {

    private final ClientRequestService clientRequestService;

    public AdminClientRequestController(ClientRequestService clientRequestService) {
        this.clientRequestService = clientRequestService;
    }

    /**
     * GET /api/admin/client-requests
     * List client requests with optional status filter (e.g. ?status=PENDING).
     */
    @GetMapping
    public ResponseEntity<Page<ClientRequestResponse>> listClientRequests(
        @RequestParam(required = false) ClientRequestStatus status,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ClientRequestResponse> page = clientRequestService.getRequests(status, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * POST /api/admin/client-requests/{id}/approve
     * Approves the request and elevates the user to CLIENT role.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ClientRequestResponse> approveClientRequest(
        @PathVariable Long id,
        @AuthenticationPrincipal User adminUser
    ) {
        ClientRequestResponse response = clientRequestService.approveRequest(id, adminUser.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/admin/client-requests/{id}/decline
     * Declines the request with a required reason.
     */
    @PostMapping("/{id}/decline")
    public ResponseEntity<ClientRequestResponse> declineClientRequest(
        @PathVariable Long id,
        @AuthenticationPrincipal User adminUser,
        @Valid @RequestBody DeclineRequest req
    ) {
        ClientRequestResponse response = clientRequestService.declineRequest(id, adminUser.getId(), req);
        return ResponseEntity.ok(response);
    }
}
