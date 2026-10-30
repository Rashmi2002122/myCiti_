package com.myciti.service;

import com.myciti.dto.GoogleUserInfo;
import com.myciti.dto.UserDto;
import com.myciti.model.*;
import com.myciti.repository.AuditLogRepository;
import com.myciti.repository.RefreshTokenRepository;
import com.myciti.repository.UserRepository;
import com.myciti.security.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogRepository auditLogRepository;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final JwtProvider jwtProvider;

    private final Set<String> adminEmails;
    private final long refreshTtlDays;
    private final int maxActiveTokensPerUser;

    @Autowired
    public AuthService(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        AuditLogRepository auditLogRepository,
        GoogleTokenVerifierService googleTokenVerifierService,
        JwtProvider jwtProvider,
        @Value("${app.auth.admin-emails:}") String adminEmailsConfig,
        @Value("${app.jwt.refresh-ttl-days:30}") long refreshTtlDays,
        @Value("${app.auth.max-active-tokens-per-user:5}") int maxActiveTokensPerUser
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditLogRepository = auditLogRepository;
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.jwtProvider = jwtProvider;
        this.adminEmails = (adminEmailsConfig != null && !adminEmailsConfig.isBlank())
            ? Arrays.stream(adminEmailsConfig.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet())
            : Collections.emptySet();
        this.refreshTtlDays = refreshTtlDays;
        this.maxActiveTokensPerUser = maxActiveTokensPerUser > 0 ? maxActiveTokensPerUser : 5;
    }

    /**
     * Authenticate via Google ID Token.
     */
    @Transactional
    public AuthResult loginWithGoogle(String idToken) {
        GoogleUserInfo googleUser = googleTokenVerifierService.verifyToken(idToken);

        // Look up user by Google subject ("sub"). If not found, check if email exists to link or create new
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

        // Enforce account suspension check BEFORE mutating state or writing to DB
        if (user.getId() != null && user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccessDeniedException("Your account is suspended. Contact admin@mycite.com");
        }

        // Update profile photo and name if changed in Google
        if (googleUser.getName() != null) user.setName(googleUser.getName());
        if (googleUser.getPictureUrl() != null) user.setPictureUrl(googleUser.getPictureUrl());
        user.setEmail(googleUser.getEmail().toLowerCase().trim());
        user.setLastLoginAt(Instant.now());

        // Role decided on server: ADMIN if email in ADMIN_EMAILS (re-checked on every login),
        // CLIENT preserved for store/business owners, otherwise default to USER (consumer)
        Role calculatedRole;
        if (adminEmails.contains(user.getEmail().toLowerCase())) {
            calculatedRole = Role.ADMIN;
        } else if (user.getRole() == Role.CLIENT) {
            calculatedRole = Role.CLIENT;
        } else {
            calculatedRole = Role.USER;
        }
        user.setRole(calculatedRole);

        user = userRepository.save(user);

        // Audit log admin logins
        if (user.getRole() == Role.ADMIN) {
            auditLogRepository.save(new AuditLog(user.getId(), "ADMIN_LOGIN", "Admin logged in via Google: " + user.getEmail()));
        }

        // 1. Purge expired or already revoked tokens for this user
        refreshTokenRepository.deleteRevokedAndExpiredForUser(user.getId(), Instant.now());

        // 2. Cap active tokens: ensure user has room for the new token
        capActiveTokensForUser(user.getId(), maxActiveTokensPerUser - 1);

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

        String tokenHash = hashToken(rawRefreshToken.trim());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new BadCredentialsException("Invalid or revoked refresh token"));

        if (storedToken.isRevoked()) {
            Instant revokedAt = storedToken.getRevokedAt();
            Duration timeSinceRevocation = (revokedAt != null) ? Duration.between(revokedAt, Instant.now()) : Duration.ofDays(999);
            boolean withinGracePeriod = revokedAt != null
                    && !revokedAt.equals(Instant.EPOCH)
                    && !timeSinceRevocation.isNegative()
                    && timeSinceRevocation.getSeconds() <= 30;

            if (withinGracePeriod) {
                log.warn("[AuthService] Refresh token invoked within {}s grace window for user #{}. Rejecting duplicate refresh without terminating user sessions.",
                    timeSinceRevocation.getSeconds(), storedToken.getUser() != null ? storedToken.getUser().getId() : null);
                throw new BadCredentialsException("Refresh token was recently rotated. Please retry with the latest token.");
            }

            User user = storedToken.getUser();
            Long userId = user != null ? user.getId() : null;
            String userEmail = user != null ? user.getEmail() : "unknown";
            log.error("[SECURITY ALERT] Refresh token reuse detected for user id #{} ({})! Possible token theft. Revoking all active sessions.",
                userId, userEmail);
            if (userId != null) {
                refreshTokenRepository.revokeAllForUser(userId, Instant.now());
            }
            throw new BadCredentialsException("Refresh token reuse detected. All sessions terminated for security.");
        }

        if (storedToken.isExpired()) {
            throw new BadCredentialsException("Refresh token is expired");
        }

        User user = storedToken.getUser();
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccessDeniedException("Your account is suspended. Contact admin@mycite.com");
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

        // Clean up dead tokens (revoked or expired) older than 5 minutes for this user
        refreshTokenRepository.deleteRevokedAndExpiredForUser(user.getId(), Instant.now().minus(Duration.ofMinutes(5)));

        // Cap active tokens for the user
        capActiveTokensForUser(user.getId(), maxActiveTokensPerUser);

        String newAccessToken = jwtProvider.generateToken(user.getId(), user.getEmail(), user.getRole(), user.isProfileComplete());
        return new AuthResult(newAccessToken, newRawRefreshToken, mapToDto(user));
    }

    private void capActiveTokensForUser(Long userId, int maxAllowed) {
        if (userId == null || maxAllowed < 0) return;
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUser_IdAndRevokedAtIsNullOrderByCreatedAtDesc(userId);
        if (activeTokens != null && activeTokens.size() > maxAllowed) {
            List<Long> idsToRevoke = activeTokens.subList(maxAllowed, activeTokens.size())
                .stream()
                .map(RefreshToken::getId)
                .toList();
            if (!idsToRevoke.isEmpty()) {
                refreshTokenRepository.revokeByIds(idsToRevoke, Instant.now());
            }
        }
    }

    /**
     * Logout: revoke the refresh token immediately with no grace period.
     */
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(rt -> {
                rt.setRevokedAt(Instant.EPOCH);
                refreshTokenRepository.save(rt);
            });
        }
    }

    String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    UserDto mapToDto(User user) {
        return UserDto.from(user);
    }

    public record AuthResult(String accessToken, String rawRefreshToken, UserDto user) {}
}
