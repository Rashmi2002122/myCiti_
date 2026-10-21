package com.myciti.controller;

import com.myciti.dto.ClientRequestResponse;
import com.myciti.dto.CreateClientRequest;
import com.myciti.model.User;
import com.myciti.service.ClientRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/client-requests")
public class ClientRequestController {

    private final ClientRequestService clientRequestService;

    public ClientRequestController(ClientRequestService clientRequestService) {
        this.clientRequestService = clientRequestService;
    }

    /**
     * POST /api/client-requests
     * Submit a "become a client" request.
     */
    @PostMapping
    public ResponseEntity<ClientRequestResponse> createClientRequest(
        @AuthenticationPrincipal User currentUser,
        @Valid @RequestBody CreateClientRequest req
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ClientRequestResponse response = clientRequestService.createRequest(currentUser.getId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/client-requests/my
     * Get the latest request status for the logged-in user.
     */
    @GetMapping("/my")
    public ResponseEntity<ClientRequestResponse> getMyClientRequest(
        @AuthenticationPrincipal User currentUser
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ClientRequestResponse response = clientRequestService.getMyLatestRequest(currentUser.getId());
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }
}
