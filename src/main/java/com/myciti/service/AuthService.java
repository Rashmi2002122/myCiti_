package com.myciti.service;

import com.myciti.dto.AuthResponse;
import com.myciti.dto.GoogleUserInfo;
import com.myciti.dto.UserDto;
import com.myciti.exception.ResourceNotFoundException;
import com.myciti.model.*;
import com.myciti.repository.AuditLogRepository;
import com.myciti.repository.RefreshTokenRepository;
import com.myciti.repository.UserRepository;
import com.myciti.security.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogRepository auditLogRepository;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final JwtProvider jwtProvider;

    private final Set<String> adminEmails;
    private final long refreshTtlDays;

    public AuthService(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        AuditLogRepository auditLogRepository,
        GoogleTokenVerifierService googleTokenVerifierService,
        JwtProvider jwtProvider,
        @Value("${app.auth.admin-emails:admin@myciti.com}") String adminEmailsConfig,
        @Value("${app.jwt.refresh-ttl-days:30}") long refreshTtlDays
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditLogRepository = auditLogRepository;
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.jwtProvider = jwtProvider;
        this.adminEmails = Arrays.stream(adminEmailsConfig.split(","))
            .map(String::trim)
            .map(String::toLowerCase)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
        this.refreshTtlDays = refreshTtlDays;
    }

    /**
     * Authenticate via Google ID Token.
     */
    @Transactional
    public AuthResult loginWithGoogle(String idToken) {
        GoogleUserInfo googleUser = googleTokenVerifierService.verifyToken(idToken);

        // 4. Look up user by Google subject ("sub"). If not found, check if email exists to link or create new
        String normalizedEmail = googleUser.getEmail().toLowerCase().trim();
        User user = userRepository.findByGoogleSub(googleUser.getSub())
            .or(() -> userRepository.findByEmail(normalizedEmail).map(existing -> {
                existing.setGoogleSub(googleUser.getSub());
                return existing;
            }))
            .orElseGet(() -> new User(
                googleUser.getSub(),
                normalizedEmail,
                googleUser.getName(),
                googleUser.getPictureUrl()
            ));

        // Update profile photo and name if changed in Google
        if (googleUser.getName() != null) user.setName(googleUser.getName());
        if (googleUser.getPictureUrl() != null) user.setPictureUrl(googleUser.getPictureUrl());
        user.setEmail(googleUser.getEmail().toLowerCase().trim());
        user.setLastLoginAt(Instant.now());

        // 5. Role decided only on server: ADMIN if email in ADMIN_EMAILS (re-checked on every login)
        Role calculatedRole = adminEmails.contains(user.getEmail().toLowerCase()) ? Role.ADMIN : Role.SUBMITTER;
        user.setRole(calculatedRole);

        user = userRepository.save(user);

        // 7. SUSPENDED users get 403
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccessDeniedException("Your account is suspended. Contact admin@myciti.com");
        }

        // Audit log admin logins
        if (user.getRole() == Role.ADMIN) {
            auditLogRepository.save(new AuditLog(user.getId(), "ADMIN_LOGIN", "Admin logged in via Google: " + user.getEmail()));
        }

        // Generate Access JWT & Refresh Token
        String accessToken = jwtProvider.generateToken(user.getId(), user.getEmail(), user.getRole(), user.isProfileComplete());
        String rawRefreshToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        String tokenHash = hashToken(rawRefreshToken);

        Instant expiresAt = Instant.now().plus(Duration.ofDays(refreshTtlDays));
        RefreshToken refreshToken = new RefreshToken(user, tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return new AuthResult(accessToken, rawRefreshToken, mapToDto(user));
    }

    /**
     * Refresh session with rotation: revoke old token and issue new pair.
     */
    @Transactional
    public AuthResult refreshSession(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BadCredentialsException("Refresh token is required");
        }

        String tokenHash = hashToken(rawRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new BadCredentialsException("Invalid or revoked refresh token"));

        if (!storedToken.isValid()) {
            throw new BadCredentialsException("Refresh token is expired or revoked");
        }

        User user = storedToken.getUser();
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccessDeniedException("Your account is suspended. Contact admin@myciti.com");
        }

        // Revoke the old refresh token (rotation)
        storedToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(storedToken);

        // Issue new refresh token
        String newRawRefreshToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        String newTokenHash = hashToken(newRawRefreshToken);
        Instant expiresAt = Instant.now().plus(Duration.ofDays(refreshTtlDays));
        RefreshToken newRefreshToken = new RefreshToken(user, newTokenHash, expiresAt);
        refreshTokenRepository.save(newRefreshToken);

        String newAccessToken = jwtProvider.generateToken(user.getId(), user.getEmail(), user.getRole(), user.isProfileComplete());
        return new AuthResult(newAccessToken, newRawRefreshToken, mapToDto(user));
    }

    /**
     * Logout: revoke the refresh token.
     */
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(rt -> {
                rt.setRevokedAt(Instant.now());
                refreshTokenRepository.save(rt);
            });
        }
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public UserDto mapToDto(User user) {
        return new UserDto(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getPictureUrl(),
            user.getAccountType(),
            user.getBusinessName(),
            user.getPhone(),
            user.getRole(),
            user.getStatus(),
            user.isProfileComplete()
        );
    }

    public record AuthResult(String accessToken, String rawRefreshToken, UserDto user) {}
}
