package com.myciti.service;

import com.myciti.dto.ClientRequestResponse;
import com.myciti.dto.CreateClientRequest;
import com.myciti.dto.DeclineRequest;
import com.myciti.model.*;
import com.myciti.repository.AuditLogRepository;
import com.myciti.repository.ClientRequestRepository;
import com.myciti.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClientRequestServiceTest {

    @Mock
    private ClientRequestRepository clientRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ClientProfileService clientProfileService;

    private ClientRequestService clientRequestService;

    private User normalUser;
    private User adminUser;
    private User clientUser;

    @BeforeEach
    void setUp() {
        clientRequestService = new ClientRequestService(clientRequestRepository, userRepository, auditLogRepository, clientProfileService);

        normalUser = new User("sub-1", "user@test.com", "Regular User", "https://img.com/u1.jpg");
        normalUser.setId(1L);
        normalUser.setRole(Role.USER);

        adminUser = new User("sub-admin", "admin@myciti.com", "Admin Person", "https://img.com/admin.jpg");
        adminUser.setId(99L);
        adminUser.setRole(Role.ADMIN);

        clientUser = new User("sub-client", "client@test.com", "Client Person", null);
        clientUser.setId(2L);
        clientUser.setRole(Role.CLIENT);
    }

    @Test
    void createRequest_individual_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(normalUser));
        when(clientRequestRepository.existsByUserIdAndStatus(1L, ClientRequestStatus.PENDING)).thenReturn(false);
        when(clientRequestRepository.save(any(ClientRequest.class))).thenAnswer(inv -> {
            ClientRequest cr = inv.getArgument(0);
            cr.setId(100L);
            return cr;
        });

        CreateClientRequest req = new CreateClientRequest(ClientType.INDIVIDUAL, null, "9830112233", "Looking to post events");
        ClientRequestResponse response = clientRequestService.createRequest(1L, req);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(ClientType.INDIVIDUAL, response.getClientType());
        assertEquals("9830112233", response.getContactPhone());
        assertEquals(ClientRequestStatus.PENDING, response.getStatus());
        assertEquals("Looking to post events", response.getNote());
    }

    @Test
    void createRequest_store_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(normalUser));
        when(clientRequestRepository.existsByUserIdAndStatus(1L, ClientRequestStatus.PENDING)).thenReturn(false);
        when(clientRequestRepository.save(any(ClientRequest.class))).thenAnswer(inv -> {
            ClientRequest cr = inv.getArgument(0);
            cr.setId(101L);
            return cr;
        });

        CreateClientRequest req = new CreateClientRequest(ClientType.STORE, "Sweet Shop", "9830112233", "Local shop");
        ClientRequestResponse response = clientRequestService.createRequest(1L, req);

        assertEquals("Sweet Shop", response.getBusinessName());
        assertEquals(ClientType.STORE, response.getClientType());
    }

    @Test
    void createRequest_storeWithoutBusinessName_throwsIllegalArgumentException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(normalUser));

        CreateClientRequest req = new CreateClientRequest(ClientType.STORE, "   ", "9830112233", "Shop");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            clientRequestService.createRequest(1L, req)
        );
        assertTrue(ex.getMessage().contains("Business name is required"));
    }

    @Test
    void createRequest_userAlreadyClient_throwsIllegalArgumentException() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(clientUser));

        CreateClientRequest req = new CreateClientRequest(ClientType.INDIVIDUAL, null, "9830112233", null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            clientRequestService.createRequest(2L, req)
        );
        assertTrue(ex.getMessage().contains("already a CLIENT"));
    }

    @Test
    void createRequest_alreadyHasPendingRequest_throwsIllegalStateException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(normalUser));
        when(clientRequestRepository.existsByUserIdAndStatus(1L, ClientRequestStatus.PENDING)).thenReturn(true);

        CreateClientRequest req = new CreateClientRequest(ClientType.INDIVIDUAL, null, "9830112233", null);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
            clientRequestService.createRequest(1L, req)
        );
        assertTrue(ex.getMessage().contains("pending client request"));
    }

    @Test
    void createRequest_suspendedUser_throwsAccessDenied() {
        normalUser.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(normalUser));

        CreateClientRequest req = new CreateClientRequest(ClientType.INDIVIDUAL, null, "9830112233", null);
        assertThrows(AccessDeniedException.class, () -> clientRequestService.createRequest(1L, req));
    }

    @Test
    void approveRequest_promotesUserToClientAndLogsAudit() {
        ClientRequest request = new ClientRequest(normalUser, ClientType.STORE, "Pandal Arts", "9830112233", "Artist");
        request.setId(50L);

        when(clientRequestRepository.findById(50L)).thenReturn(Optional.of(request));
        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));
        when(clientRequestRepository.save(any(ClientRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientRequestResponse response = clientRequestService.approveRequest(50L, 99L);

        assertEquals(ClientRequestStatus.APPROVED, response.getStatus());
        assertEquals(99L, response.getReviewedBy());
        assertNotNull(response.getReviewedAt());

        // Verify User updated
        assertEquals(Role.CLIENT, normalUser.getRole());
        assertEquals(ClientType.STORE, normalUser.getClientType());
        assertEquals("Pandal Arts", normalUser.getBusinessName());
        assertEquals("9830112233", normalUser.getPhone());
        verify(userRepository).save(normalUser);

        // Verify Audit Log
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertEquals("CLIENT_REQUEST_APPROVED", auditCaptor.getValue().getAction());
    }

    @Test
    void declineRequest_setsDeclinedAndDoesNotChangeUserRole() {
        ClientRequest request = new ClientRequest(normalUser, ClientType.STORE, "Pandal Arts", "9830112233", "Artist");
        request.setId(51L);

        when(clientRequestRepository.findById(51L)).thenReturn(Optional.of(request));
        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));
        when(clientRequestRepository.save(any(ClientRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        DeclineRequest declineReq = new DeclineRequest("Incomplete documentation provided. Please provide valid contact proof.");
        ClientRequestResponse response = clientRequestService.declineRequest(51L, 99L, declineReq);

        assertEquals(ClientRequestStatus.DECLINED, response.getStatus());
        assertEquals(99L, response.getReviewedBy());
        assertEquals("Incomplete documentation provided. Please provide valid contact proof.", response.getDeclineReason());

        // Verify user role NOT changed
        assertEquals(Role.USER, normalUser.getRole());
        verify(userRepository, never()).save(any(User.class));

        // Verify audit log
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void declineRequest_withoutReason_throwsIllegalArgumentException() {
        ClientRequest request = new ClientRequest(normalUser, ClientType.STORE, "Pandal Arts", "9830112233", "Artist");
        request.setId(52L);
        when(clientRequestRepository.findById(52L)).thenReturn(Optional.of(request));

        DeclineRequest declineReq = new DeclineRequest("   ");
        assertThrows(IllegalArgumentException.class, () -> clientRequestService.declineRequest(52L, 99L, declineReq));
    }

    @Test
    void declineRequest_alreadyApproved_throwsIllegalStateException() {
        ClientRequest request = new ClientRequest(normalUser, ClientType.STORE, "Pandal Arts", "9830112233", "Artist");
        request.setId(53L);
        request.setStatus(ClientRequestStatus.APPROVED);
        when(clientRequestRepository.findById(53L)).thenReturn(Optional.of(request));

        DeclineRequest declineReq = new DeclineRequest("Cannot decline now");
        assertThrows(IllegalStateException.class, () -> clientRequestService.declineRequest(53L, 99L, declineReq));
    }
}
