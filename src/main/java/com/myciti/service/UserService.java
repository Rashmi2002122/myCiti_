package com.myciti.service;

import com.myciti.dto.AuditLogDto;
import com.myciti.dto.UpdateLocationRequest;
import com.myciti.dto.UpdateProfileRequest;
import com.myciti.dto.UserCountsDto;
import com.myciti.dto.UserDto;
import com.myciti.event.UserStatusChangedEvent;
import com.myciti.exception.ResourceNotFoundException;
import com.myciti.model.*;
import com.myciti.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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
    private final UserSavedListingRepository userSavedListingRepository;
    private final SubmissionRepository submissionRepository;
    private final ClientRequestRepository clientRequestRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        AuditLogRepository auditLogRepository,
        PushSubscriptionRepository pushSubscriptionRepository,
        UserSavedListingRepository userSavedListingRepository,
        SubmissionRepository submissionRepository,
        ClientRequestRepository clientRequestRepository
    ) {
        this(userRepository, refreshTokenRepository, auditLogRepository, pushSubscriptionRepository, userSavedListingRepository, submissionRepository, clientRequestRepository, null);
    }

    @Autowired
    public UserService(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        AuditLogRepository auditLogRepository,
        PushSubscriptionRepository pushSubscriptionRepository,
        UserSavedListingRepository userSavedListingRepository,
        SubmissionRepository submissionRepository,
        ClientRequestRepository clientRequestRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditLogRepository = auditLogRepository;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.userSavedListingRepository = userSavedListingRepository;
        this.submissionRepository = submissionRepository;
        this.clientRequestRepository = clientRequestRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Get user profile by ID including dynamic counts for menu.
     */
    @Transactional(readOnly = true)
    public UserDto getProfile(Long userId) {
        User user = findUserById(userId);
        return mapToDto(user);
    }

    /**
     * Update user basic profile.
     * Enforces:
     * - Role cannot be changed via profile updates (promotions only via admin client request approval).
     * - Name and Phone number sanitized.
     * - For CLIENT users: clientType and businessName can be updated.
     * - Sets profileComplete = true.
     */
    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccessDeniedException("Your account is suspended. Contact admin@myciti.com");
        }

        String name = sanitize(request.getName());
        if (name != null) {
            user.setName(name);
        }

        String phone = sanitize(request.getPhone());
        if (phone != null) {
            user.setPhone(phone);
        }

        // Only CLIENT accounts manage clientType & businessName here
        if (user.getRole() == Role.CLIENT) {
            ClientType clientType = request.getClientType();
            String businessName = sanitize(request.getBusinessName());

            if (clientType != null) {
                user.setClientType(clientType);
            }

            if (user.getClientType() == ClientType.STORE || user.getClientType() == ClientType.COMPANY) {
                if (businessName != null && !businessName.isBlank()) {
                    user.setBusinessName(businessName);
                }
            } else if (user.getClientType() == ClientType.INDIVIDUAL) {
                if (businessName != null && businessName.isBlank()) {
                    user.setBusinessName(null);
                } else if (businessName != null) {
                    user.setBusinessName(businessName);
                }
            }
        }

        user.setProfileComplete(true);

        User saved = userRepository.save(user);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new UserStatusChangedEvent(userId));
        }
        return mapToDto(saved);
    }

    /**
     * Update user coarse location and city for geo-targeted notifications.
     */
    @Transactional
    public UserDto updateLocation(Long userId, UpdateLocationRequest request) {
        User user = findUserById(userId);

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccessDeniedException("Your account is suspended. Contact admin@myciti.com");
        }

        if (request.getLatitude() != null) {
            user.setCoarseLat(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            user.setCoarseLng(request.getLongitude());
        }
        if (request.getCity() != null && !request.getCity().isBlank()) {
            user.setAreaName(sanitize(request.getCity()));
        }
        user.setLocationUpdatedAt(Instant.now());

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

            if (eventPublisher != null) {
                eventPublisher.publishEvent(new UserStatusChangedEvent(targetUserId));
            }
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

            if (eventPublisher != null) {
                eventPublisher.publishEvent(new UserStatusChangedEvent(targetUserId));
            }
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

    public UserCountsDto buildCountsForUser(User user) {
        if (user == null || user.getId() == null) {
            return new UserCountsDto(0L);
        }
        Long userId = user.getId();
        UserCountsDto counts = new UserCountsDto();
        counts.setSavedCount(userSavedListingRepository.countByUserId(userId));

        if (user.getRole() == Role.CLIENT || user.getRole() == Role.ADMIN) {
            counts.setPendingCount(submissionRepository.countByUserIdAndStatus(userId, SubmissionStatus.PENDING));
            counts.setApprovedCount(submissionRepository.countByUserIdAndStatus(userId, SubmissionStatus.APPROVED));
            counts.setDeclinedCount(submissionRepository.countByUserIdAndStatus(userId, SubmissionStatus.DECLINED));
        }

        if (user.getRole() == Role.ADMIN) {
            counts.setPendingReviewCount(submissionRepository.countByStatus(SubmissionStatus.PENDING));
            counts.setPendingClientRequestCount(clientRequestRepository.countByStatus(ClientRequestStatus.PENDING));
        }

        return counts;
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User #" + id + " not found"));
    }

    private String sanitize(String input) {
        return com.myciti.util.SanitizeUtils.cleanText(input);
    }

    private UserDto mapToDto(User user) {
        UserDto dto = UserDto.from(user);
        if (user != null && user.getId() != null) {
            dto.setCounts(buildCountsForUser(user));
        }
        return dto;
    }
}
