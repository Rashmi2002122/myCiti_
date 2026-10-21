package com.myciti.service;

import com.myciti.dto.UpdateLocationRequest;
import com.myciti.dto.UpdateProfileRequest;
import com.myciti.dto.UserDto;
import com.myciti.model.*;
import com.myciti.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private PushSubscriptionRepository pushSubscriptionRepository;

    @Mock
    private UserSavedListingRepository userSavedListingRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ClientRequestRepository clientRequestRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
            userRepository,
            refreshTokenRepository,
            auditLogRepository,
            pushSubscriptionRepository,
            userSavedListingRepository,
            submissionRepository,
            clientRequestRepository
        );
    }

    @Test
    void getProfile_userRole_returnsOnlySavedCount() {
        User user = new User("sub-1", "user@test.com", "Name", null);
        user.setId(1L);
        user.setRole(Role.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userSavedListingRepository.countByUserId(1L)).thenReturn(5L);

        UserDto dto = userService.getProfile(1L);

        assertNotNull(dto.getCounts());
        assertEquals(5L, dto.getCounts().getSavedCount());
        assertNull(dto.getCounts().getPendingCount());
        assertNull(dto.getCounts().getApprovedCount());
        assertNull(dto.getCounts().getDeclinedCount());
        assertNull(dto.getCounts().getPendingReviewCount());
        assertNull(dto.getCounts().getPendingClientRequestCount());
    }

    @Test
    void getProfile_clientRole_returnsClientCounts() {
        User client = new User("sub-client", "client@test.com", "Client", null);
        client.setId(2L);
        client.setRole(Role.CLIENT);
        when(userRepository.findById(2L)).thenReturn(Optional.of(client));
        when(userSavedListingRepository.countByUserId(2L)).thenReturn(3L);
        when(submissionRepository.countByUserIdAndStatus(2L, SubmissionStatus.PENDING)).thenReturn(1L);
        when(submissionRepository.countByUserIdAndStatus(2L, SubmissionStatus.APPROVED)).thenReturn(4L);
        when(submissionRepository.countByUserIdAndStatus(2L, SubmissionStatus.DECLINED)).thenReturn(0L);

        UserDto dto = userService.getProfile(2L);

        assertNotNull(dto.getCounts());
        assertEquals(3L, dto.getCounts().getSavedCount());
        assertEquals(1L, dto.getCounts().getPendingCount());
        assertEquals(4L, dto.getCounts().getApprovedCount());
        assertEquals(0L, dto.getCounts().getDeclinedCount());
        assertNull(dto.getCounts().getPendingReviewCount());
        assertNull(dto.getCounts().getPendingClientRequestCount());
    }

    @Test
    void getProfile_adminRole_returnsAllCounts() {
        User admin = new User("sub-admin", "admin@test.com", "Admin", null);
        admin.setId(99L);
        admin.setRole(Role.ADMIN);
        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
        when(userSavedListingRepository.countByUserId(99L)).thenReturn(2L);
        when(submissionRepository.countByUserIdAndStatus(99L, SubmissionStatus.PENDING)).thenReturn(0L);
        when(submissionRepository.countByUserIdAndStatus(99L, SubmissionStatus.APPROVED)).thenReturn(10L);
        when(submissionRepository.countByUserIdAndStatus(99L, SubmissionStatus.DECLINED)).thenReturn(1L);
        when(submissionRepository.countByStatus(SubmissionStatus.PENDING)).thenReturn(7L);
        when(clientRequestRepository.countByStatus(ClientRequestStatus.PENDING)).thenReturn(3L);

        UserDto dto = userService.getProfile(99L);

        assertNotNull(dto.getCounts());
        assertEquals(2L, dto.getCounts().getSavedCount());
        assertEquals(0L, dto.getCounts().getPendingCount());
        assertEquals(10L, dto.getCounts().getApprovedCount());
        assertEquals(1L, dto.getCounts().getDeclinedCount());
        assertEquals(7L, dto.getCounts().getPendingReviewCount());
        assertEquals(3L, dto.getCounts().getPendingClientRequestCount());
    }

    @Test
    void updateProfile_user_cannotSelfEscalateToClient() {
        User user = new User("sub-1", "user@test.com", "Regular User", null);
        user.setId(1L);
        user.setRole(Role.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest(AccountType.STORE, "Attempted Store", "9830112233");
        UserDto result = userService.updateProfile(1L, request);

        assertTrue(result.isProfileComplete());
        assertEquals("9830112233", result.getPhone());
        // Crucial security invariant: Role remains USER!
        assertEquals(Role.USER, result.getRole());
        assertNull(result.getBusinessName());
    }

    @Test
    void updateProfile_clientCanUpdateBusinessName() {
        User client = new User("sub-client", "client@test.com", "Business Owner", null);
        client.setId(10L);
        client.setRole(Role.CLIENT);
        client.setClientType(ClientType.STORE);
        when(userRepository.findById(10L)).thenReturn(Optional.of(client));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest("Updated Store Name", ClientType.STORE, "New Store", "9830112233");
        UserDto result = userService.updateProfile(10L, request);

        assertTrue(result.isProfileComplete());
        assertEquals("New Store", result.getBusinessName());
        assertEquals(ClientType.STORE, result.getClientType());
        assertEquals(Role.CLIENT, result.getRole());
    }

    @Test
    void updateProfile_adminUser_retainsAdminRole() {
        User user = new User("sub-admin", "admin@test.com", "Admin Person", null);
        user.setId(99L);
        user.setRole(Role.ADMIN);
        when(userRepository.findById(99L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest(AccountType.INDIVIDUAL, null, "9830112233");
        UserDto result = userService.updateProfile(99L, request);

        assertEquals(Role.ADMIN, result.getRole());
    }

    @Test
    void updateProfile_suspendedUser_throwsAccessDenied() {
        User user = new User("sub-3", "suspended@test.com", "Suspended", null);
        user.setId(3L);
        user.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        UpdateProfileRequest request = new UpdateProfileRequest(AccountType.INDIVIDUAL, null, "9830112233");
        assertThrows(AccessDeniedException.class, () -> userService.updateProfile(3L, request));
    }

    @Test
    void suspendUser_revokesAllTokensAndLogsAudit() {
        User target = new User("sub-4", "target@test.com", "Target", null);
        target.setId(4L);
        when(userRepository.findById(4L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto suspended = userService.suspendUser(100L, 4L);

        assertEquals(UserStatus.SUSPENDED, suspended.getStatus());
        verify(refreshTokenRepository).revokeAllForUser(eq(4L), any(Instant.class));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void unsuspendUser_updatesStatusAndLogsAudit() {
        User target = new User("sub-4", "target@test.com", "Target", null);
        target.setId(4L);
        target.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findById(4L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto unsuspended = userService.unsuspendUser(100L, 4L);

        assertEquals(UserStatus.ACTIVE, unsuspended.getStatus());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void updateLocation_success() {
        User user = new User("sub-loc", "loc@test.com", "Location User", null);
        user.setId(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateLocationRequest req = new UpdateLocationRequest(22.5726, 88.3639, "Kolkata");
        UserDto result = userService.updateLocation(5L, req);

        assertEquals(22.5726, result.getCoarseLat());
        assertEquals(88.3639, result.getCoarseLng());
        assertEquals("Kolkata", result.getAreaName());
        assertNotNull(result.getLocationUpdatedAt());
    }

    @Test
    void updateLocation_suspendedUser_throwsAccessDenied() {
        User user = new User("sub-suspended", "bad@test.com", "Bad User", null);
        user.setId(6L);
        user.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findById(6L)).thenReturn(Optional.of(user));

        UpdateLocationRequest req = new UpdateLocationRequest(22.5726, 88.3639, "Kolkata");
        assertThrows(AccessDeniedException.class, () -> userService.updateLocation(6L, req));
    }
}