package com.myciti.service;

import com.myciti.dto.UpdateProfileRequest;
import com.myciti.dto.UserDto;
import com.myciti.model.AccountType;
import com.myciti.model.AuditLog;
import com.myciti.model.User;
import com.myciti.model.UserStatus;
import com.myciti.repository.AuditLogRepository;
import com.myciti.repository.RefreshTokenRepository;
import com.myciti.repository.UserRepository;
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

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, refreshTokenRepository, auditLogRepository);
    }

    @Test
    void updateProfile_individual_success() {
        User user = new User("sub-1", "user@test.com", "Name", null);
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest(AccountType.INDIVIDUAL, null, "9830112233");
        UserDto result = userService.updateProfile(1L, request);

        assertTrue(result.isProfileComplete());
        assertEquals(AccountType.INDIVIDUAL, result.getAccountType());
        assertEquals("9830112233", result.getPhone());
        assertNull(result.getBusinessName());
    }

    @Test
    void updateProfile_storeWithoutBusinessName_throwsIllegalArgumentException() {
        User user = new User("sub-2", "store@test.com", "Store Owner", null);
        user.setId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        UpdateProfileRequest request = new UpdateProfileRequest(AccountType.STORE, "   ", "9830112233");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            userService.updateProfile(2L, request)
        );
        assertTrue(ex.getMessage().contains("Business name is required"));
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
}
