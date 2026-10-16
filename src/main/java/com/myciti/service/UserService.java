package com.myciti.service;

import com.myciti.dto.AuditLogDto;
import com.myciti.dto.UpdateProfileRequest;
import com.myciti.dto.UserDto;
import com.myciti.exception.ResourceNotFoundException;
import com.myciti.model.AccountType;
import com.myciti.model.AuditLog;
import com.myciti.model.Role;
import com.myciti.model.User;
import com.myciti.model.UserStatus;
import com.myciti.repository.AuditLogRepository;
import com.myciti.repository.PushSubscriptionRepository;
import com.myciti.repository.RefreshTokenRepository;
import com.myciti.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogRepository auditLogRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;

    public UserService(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        AuditLogRepository auditLogRepository,
        PushSubscriptionRepository pushSubscriptionRepository
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditLogRepository = auditLogRepository;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
    }

    /**
     * Get user profile by ID.
     */
    @Transactional(readOnly = true)
    public UserDto getProfile(Long userId) {
        User user = findUserById(userId);
        return mapToDto(user);
    }

    /**
     * Update user profile.
     * Enforces:
     * - businessName is required for STORE and COMPANY
     * - Phone number is stripped and validated
     * - All text inputs are HTML stripped & trimmed
     * - Sets profileComplete = true
     */
    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccessDeniedException("Your account is suspended. Contact admin@myciti.com");
        }

        AccountType accountType = request.getAccountType();
        String businessName = sanitize(request.getBusinessName());
        String phone = sanitize(request.getPhone());

        // Validate businessName for STORE and COMPANY
        if ((accountType == AccountType.STORE || accountType == AccountType.COMPANY)) {
            if (businessName == null || businessName.isBlank()) {
                throw new IllegalArgumentException("Business name is required for " + accountType + " accounts");
            }
        } else {
            // For INDIVIDUAL accounts, businessName is not required
            if (businessName != null && businessName.isBlank()) {
                businessName = null;
            }
        }

        user.setAccountType(accountType);
        user.setBusinessName(businessName);
        user.setPhone(phone);
        user.setProfileComplete(true);

        // Role transitions:
        // - ADMIN role is immutable through profile updates.
        // - STORE or COMPANY accounts are business owners -> Role.CLIENT.
        // - INDIVIDUAL accounts are regular consumers -> Role.USER.
        if (user.getRole() != Role.ADMIN) {
            if (accountType == AccountType.STORE || accountType == AccountType.COMPANY) {
                user.setRole(Role.CLIENT);
            } else {
                user.setRole(Role.USER);
            }
        }

        User saved = userRepository.save(user);
        return mapToDto(saved);
    }

    /**
     * Admin: Search/List users with pagination.
     */
    @Transactional(readOnly = true)
    public Page<UserDto> searchUsers(String query, Pageable pageable) {
        String trimmedQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        return userRepository.searchUsers(trimmedQuery, pageable).map(this::mapToDto);
    }

    /**
     * Admin: Suspend user.
     */
    @Transactional
    public UserDto suspendUser(Long adminUserId, Long targetUserId) {
        User targetUser = findUserById(targetUserId);

        if (targetUser.getStatus() != UserStatus.SUSPENDED) {
            targetUser.setStatus(UserStatus.SUSPENDED);
            userRepository.save(targetUser);

            // Revoke all active refresh tokens for the suspended user immediately
            refreshTokenRepository.revokeAllForUser(targetUserId, Instant.now());

            // Audit log
            auditLogRepository.save(new AuditLog(
                adminUserId,
                "USER_SUSPENDED",
                "User #" + targetUserId + " (" + targetUser.getEmail() + ") was suspended by admin #" + adminUserId
            ));
        }

        return mapToDto(targetUser);
    }

    /**
     * Admin: Unsuspend user.
     */
    @Transactional
    public UserDto unsuspendUser(Long adminUserId, Long targetUserId) {
        User targetUser = findUserById(targetUserId);

        if (targetUser.getStatus() != UserStatus.ACTIVE) {
            targetUser.setStatus(UserStatus.ACTIVE);
            userRepository.save(targetUser);

            // Audit log
            auditLogRepository.save(new AuditLog(
                adminUserId,
                "USER_UNSUSPENDED",
                "User #" + targetUserId + " (" + targetUser.getEmail() + ") was unsuspended by admin #" + adminUserId
            ));
        }

        return mapToDto(targetUser);
    }

    /**
     * Admin: Get audit logs.
     */
    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
            .map(log -> new AuditLogDto(
                log.getId(),
                log.getActorUserId(),
                log.getAction(),
                log.getDetails(),
                log.getCreatedAt()
            ));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User #" + id + " not found"));
    }

    private String sanitize(String input) {
        if (input == null) return null;
        // Strip HTML tags and trim
        String cleaned = input.replaceAll("<[^>]*>", "").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private UserDto mapToDto(User user) {
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
}
