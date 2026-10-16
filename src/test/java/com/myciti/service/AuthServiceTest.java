package com.myciti.service;

import com.myciti.dto.GoogleUserInfo;
import com.myciti.model.*;
import com.myciti.repository.AuditLogRepository;
import com.myciti.repository.RefreshTokenRepository;
import com.myciti.repository.UserRepository;
import com.myciti.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private GoogleTokenVerifierService googleTokenVerifierService;

    @Mock
    private JwtProvider jwtProvider;

    private AuthService authService;

    private final String adminEmails = "admin@myciti.com,super@myciti.com";

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            userRepository,
            refreshTokenRepository,
            auditLogRepository,
            googleTokenVerifierService,
            jwtProvider,
            adminEmails,
            30,
            5
        );
    }

    @Test
    void loginWithGoogle_newUser_assignedUserRole() {
        GoogleUserInfo googleUser = new GoogleUserInfo("sub-123", "user@example.com", true, "John Doe", "https://pic.jpg");
        when(googleTokenVerifierService.verifyToken("valid-token")).thenReturn(googleUser);
        when(userRepository.findByGoogleSub("sub-123")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtProvider.generateToken(any(), any(), any(), anyBoolean())).thenReturn("mock-access-token");

        AuthService.AuthResult result = authService.loginWithGoogle("valid-token");

        assertNotNull(result);
        assertEquals("mock-access-token", result.accessToken());
        assertEquals(Role.USER, result.user().getRole());
        assertEquals("user@example.com", result.user().getEmail());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void loginWithGoogle_existingClient_preservesClientRole() {
        User clientUser = new User("sub-client", "client@example.com", "Client Store", null);
        clientUser.setId(2L);
        clientUser.setRole(Role.CLIENT);

        GoogleUserInfo googleUser = new GoogleUserInfo("sub-client", "client@example.com", true, "Client Store", null);
        when(googleTokenVerifierService.verifyToken("client-token")).thenReturn(googleUser);
        when(userRepository.findByGoogleSub("sub-client")).thenReturn(Optional.of(clientUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtProvider.generateToken(any(), any(), any(), anyBoolean())).thenReturn("client-access-token");

        AuthService.AuthResult result = authService.loginWithGoogle("client-token");

        assertNotNull(result);
        assertEquals(Role.CLIENT, result.user().getRole());
    }

    @Test
    void loginWithGoogle_emailInAdminEmails_assignedAdminRoleAndAuditLogged() {
        GoogleUserInfo googleUser = new GoogleUserInfo("sub-admin", "admin@myciti.com", true, "Admin User", null);
        when(googleTokenVerifierService.verifyToken("admin-token")).thenReturn(googleUser);
        when(userRepository.findByGoogleSub("sub-admin")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });
        when(jwtProvider.generateToken(any(), any(), any(), anyBoolean())).thenReturn("admin-access-token");

        AuthService.AuthResult result = authService.loginWithGoogle("admin-token");

        assertEquals(Role.ADMIN, result.user().getRole());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void loginWithGoogle_emailRemovedFromAdminEmails_downgradesToUser() {
        User existingUser = new User("sub-former-admin", "formeradmin@example.com", "Former Admin", null);
        existingUser.setId(10L);
        existingUser.setRole(Role.ADMIN);

        GoogleUserInfo googleUser = new GoogleUserInfo("sub-former-admin", "formeradmin@example.com", true, "Former Admin", null);
        when(googleTokenVerifierService.verifyToken("token")).thenReturn(googleUser);
        when(userRepository.findByGoogleSub("sub-former-admin")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtProvider.generateToken(any(), any(), any(), anyBoolean())).thenReturn("token");

        AuthService.AuthResult result = authService.loginWithGoogle("token");

        assertEquals(Role.USER, result.user().getRole());
    }

    @Test
    void loginWithGoogle_suspendedUser_throws403AccessDenied() {
        User suspendedUser = new User("sub-suspended", "bad@example.com", "Bad Actor", null);
        suspendedUser.setId(5L);
        suspendedUser.setStatus(UserStatus.SUSPENDED);

        GoogleUserInfo googleUser = new GoogleUserInfo("sub-suspended", "bad@example.com", true, "Bad Actor", null);
        when(googleTokenVerifierService.verifyToken("bad-token")).thenReturn(googleUser);
        when(userRepository.findByGoogleSub("sub-suspended")).thenReturn(Optional.of(suspendedUser));

        assertThrows(AccessDeniedException.class, () -> authService.loginWithGoogle("bad-token"));
    }

    @Test
    void refreshSession_validToken_rotatesAndReturnsNewTokens() {
        User user = new User("sub-1", "user@example.com", "User", null);
        user.setId(1L);

        RefreshToken oldToken = new RefreshToken(user, "old-hash", Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(oldToken));
        when(jwtProvider.generateToken(any(), any(), any(), anyBoolean())).thenReturn("new-access-token");

        AuthService.AuthResult result = authService.refreshSession("raw-refresh-token");

        assertNotNull(result);
        assertEquals("new-access-token", result.accessToken());
        assertNotNull(oldToken.getRevokedAt()); // verified rotated
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refreshSession_revokedOrExpiredToken_throwsBadCredentials() {
        User user = new User("sub-1", "user@example.com", "User", null);
        RefreshToken revokedToken = new RefreshToken(user, "revoked-hash", Instant.now().plusSeconds(3600));
        revokedToken.setRevokedAt(Instant.now());

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revokedToken));

        assertThrows(BadCredentialsException.class, () -> authService.refreshSession("raw-token"));
    }

    @Test
    void refreshSession_suspendedUser_throws403() {
        User user = new User("sub-1", "user@example.com", "User", null);
        user.setStatus(UserStatus.SUSPENDED);
        RefreshToken validToken = new RefreshToken(user, "valid-hash", Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(validToken));

        assertThrows(AccessDeniedException.class, () -> authService.refreshSession("raw-token"));
    }

    @Test
    void loginWithGoogle_exceedsMaxActiveTokens_revokesOldestTokens() {
        GoogleUserInfo googleUser = new GoogleUserInfo("sub-cap", "cap@example.com", true, "Cap User", null);
        when(googleTokenVerifierService.verifyToken("cap-token")).thenReturn(googleUser);

        User user = new User("sub-cap", "cap@example.com", "Cap User", null);
        user.setId(50L);
        when(userRepository.findByGoogleSub("sub-cap")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtProvider.generateToken(any(), any(), any(), anyBoolean())).thenReturn("cap-access-token");

        // Simulate user already having 5 active tokens (exceeding cap of 4 before adding new)
        java.util.List<RefreshToken> existingActive = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            RefreshToken token = new RefreshToken(user, "hash-" + i, java.time.Instant.now().plusSeconds(3600));
            token.setId((long) (i + 1));
            existingActive.add(token);
        }
        when(refreshTokenRepository.findByUser_IdAndRevokedAtIsNullOrderByCreatedAtDesc(50L)).thenReturn(existingActive);

        authService.loginWithGoogle("cap-token");

        // Oldest token (at index 4 with ID 5L) should have been revoked via repository
        verify(refreshTokenRepository).revokeByIds(eq(java.util.List.of(5L)), any(Instant.class));
    }
}
