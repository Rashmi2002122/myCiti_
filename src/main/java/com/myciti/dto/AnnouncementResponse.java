package com.myciti.dto;

import com.myciti.model.Announcement;
import com.myciti.model.AnnouncementStatus;
import com.myciti.model.TargetType;

import java.time.Instant;

public record AnnouncementResponse(
    Long id,
    Long listingId,
    String listingTitle,
    Long authorUserId,
    String authorName,
    String authorRole,
    String title,
    String message,
    TargetType targetType,
    Double targetLat,
    Double targetLng,
    Double radiusKm,
    String targetCity,
    AnnouncementStatus status,
    String rejectionReason,
    Instant sentAt,
    Integer recipientCount,
    Instant createdAt
) {
    public static AnnouncementResponse fromEntity(Announcement a) {
        return new AnnouncementResponse(
            a.getId(),
            a.getListing() != null ? a.getListing().getId() : null,
            a.getListing() != null ? a.getListing().getTitle() : null,
            a.getAuthorUser() != null ? a.getAuthorUser().getId() : null,
            a.getAuthorUser() != null ? a.getAuthorUser().getName() : null,
            a.getAuthorUser() != null && a.getAuthorUser().getRole() != null ? a.getAuthorUser().getRole().name() : null,
            a.getTitle(),
            a.getMessage(),
            a.getTargetType(),
            a.getTargetLat(),
            a.getTargetLng(),
            a.getRadiusKm(),
            a.getTargetCity(),
            a.getStatus(),
            a.getRejectionReason(),
            a.getSentAt(),
            a.getRecipientCount(),
            a.getCreatedAt()
        );
    }
}
