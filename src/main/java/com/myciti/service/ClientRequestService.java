package com.myciti.service;

import com.myciti.dto.ClientRequestResponse;
import com.myciti.dto.CreateClientRequest;
import com.myciti.dto.DeclineRequest;
import com.myciti.exception.ResourceNotFoundException;
import com.myciti.model.*;
import com.myciti.repository.AuditLogRepository;
import com.myciti.repository.ClientRequestRepository;
import com.myciti.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ClientRequestService {

    private final ClientRequestRepository clientRequestRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final ClientProfileService clientProfileService;

    public ClientRequestService(
        ClientRequestRepository clientRequestRepository,
        UserRepository userRepository,
        AuditLogRepository auditLogRepository,
        ClientProfileService clientProfileService
    ) {
        this.clientRequestRepository = clientRequestRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.clientProfileService = clientProfileService;
    }

    /**
     * Submit a new ClientRequest.
     * Enforces:
     * - User must exist and not be suspended.
     * - User cannot already be a CLIENT or ADMIN.
     * - Only one open (PENDING) request per user at any time.
     * - businessName is required for STORE and COMPANY clientTypes.
     */
    @Transactional
    public ClientRequestResponse createRequest(Long userId, CreateClientRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User #" + userId + " not found"));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccessDeniedException("Your account is suspended. Contact admin@myciti.com");
        }

        if (user.getRole() == Role.CLIENT || user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("User is already a " + user.getRole());
        }

        if (clientRequestRepository.existsByUserIdAndStatus(userId, ClientRequestStatus.PENDING)) {
            throw new IllegalStateException("You already have a pending client request under review");
        }

        ClientType clientType = req.getClientType();
        String businessName = sanitize(req.getBusinessName());
        String contactPhone = sanitize(req.getContactPhone());
        String note = sanitize(req.getNote());

        if (clientType == ClientType.STORE || clientType == ClientType.COMPANY) {
            if (businessName == null || businessName.isBlank()) {
                throw new IllegalArgumentException("Business name is required for " + clientType + " accounts");
            }
        } else {
            if (businessName != null && businessName.isBlank()) {
                businessName = null;
            }
        }

        ClientRequest clientRequest = new ClientRequest();
        clientRequest.setUser(user);
        clientRequest.setClientType(clientType);
        clientRequest.setBusinessName(businessName);
        clientRequest.setContactPhone(contactPhone);
        clientRequest.setNote(note);
        clientRequest.setStatus(ClientRequestStatus.PENDING);
        clientRequest.setCreatedAt(Instant.now());
        clientRequest.setUpdatedAt(Instant.now());

        ClientRequest saved = clientRequestRepository.save(clientRequest);
        return ClientRequestResponse.fromEntity(saved);
    }

    /**
     * Get current user's latest client request (to view pending or declined status).
     */
    @Transactional(readOnly = true)
    public ClientRequestResponse getMyLatestRequest(Long userId) {
        return clientRequestRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
            .map(ClientRequestResponse::fromEntity)
            .orElse(null);
    }

    /**
     * Admin: List client requests with optional status filter.
     */
    @Transactional(readOnly = true)
    public Page<ClientRequestResponse> getRequests(ClientRequestStatus status, Pageable pageable) {
        Page<ClientRequest> page;
        if (status != null) {
            page = clientRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            page = clientRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return page.map(ClientRequestResponse::fromEntity);
    }

    /**
     * Admin: Approve client request.
     * Changes role of user to CLIENT and updates user's clientType and businessName.
     */
    @Transactional
    public ClientRequestResponse approveRequest(Long requestId, Long adminUserId) {
        ClientRequest req = clientRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("ClientRequest #" + requestId + " not found"));

        if (req.getStatus() == ClientRequestStatus.APPROVED) {
            return ClientRequestResponse.fromEntity(req);
        }

        User adminUser = userRepository.findById(adminUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Admin user #" + adminUserId + " not found"));

        req.setStatus(ClientRequestStatus.APPROVED);
        req.setDeclineReason(null);
        req.setReviewedBy(adminUser);
        req.setReviewedAt(Instant.now());
        req.setUpdatedAt(Instant.now());

        User user = req.getUser();
        user.setRole(Role.CLIENT);
        user.setClientType(req.getClientType());
        if (req.getBusinessName() != null && !req.getBusinessName().isBlank()) {
            user.setBusinessName(req.getBusinessName());
        }
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            user.setPhone(req.getContactPhone());
        }
        User savedUser = userRepository.save(user);

        // Auto-create client profile for the approved client
        clientProfileService.createProfileForClient(
            savedUser,
            req.getBusinessName(),
            req.getClientType(),
            req.getContactPhone()
        );

        ClientRequest saved = clientRequestRepository.save(req);

        auditLogRepository.save(new AuditLog(
            adminUserId,
            "CLIENT_REQUEST_APPROVED",
            "Approved client request #" + requestId + " for user #" + user.getId() + " (" + user.getEmail() + ")"
        ));

        return ClientRequestResponse.fromEntity(saved);
    }

    /**
     * Admin: Decline client request with required reason.
     */
    @Transactional
    public ClientRequestResponse declineRequest(Long requestId, Long adminUserId, DeclineRequest declineReq) {
        ClientRequest req = clientRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("ClientRequest #" + requestId + " not found"));

        if (req.getStatus() != ClientRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be declined (current status: " + req.getStatus() + ")");
        }

        String reason = sanitize(declineReq.getReason());
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Decline reason is required");
        }

        User adminUser = userRepository.findById(adminUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Admin user #" + adminUserId + " not found"));

        req.setStatus(ClientRequestStatus.DECLINED);
        req.setDeclineReason(reason);
        req.setReviewedBy(adminUser);
        req.setReviewedAt(Instant.now());
        req.setUpdatedAt(Instant.now());

        ClientRequest saved = clientRequestRepository.save(req);

        auditLogRepository.save(new AuditLog(
            adminUserId,
            "CLIENT_REQUEST_DECLINED",
            "Declined client request #" + requestId + " for user #" + req.getUser().getId() + ". Reason: " + reason
        ));

        return ClientRequestResponse.fromEntity(saved);
    }

    private String sanitize(String input) {
        if (input == null) return null;
        String cleaned = input.replaceAll("<[^>]*>", "").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
