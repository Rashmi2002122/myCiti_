package com.myciti.dto;

import com.myciti.model.AdCampaign;
import com.myciti.model.CampaignStatus;
import com.myciti.model.TargetType;

import java.time.Instant;
import java.time.LocalDate;

public record AdCampaignResponse(
    Long id,
    Long listingId,
    String listingTitle,
    String listingCategory,
    String listingCity,
    Long clientUserId,
    String clientName,
    String title,
    String headline,
    TargetType targetType,
    Double targetLat,
    Double targetLng,
    Double radiusKm,
    String targetCity,
    CampaignStatus status,
    String rejectionReason,
    LocalDate startDate,
    LocalDate endDate,
    Long impressionsCount,
    Long clicksCount,
    Instant createdAt,
    Instant updatedAt
) {
    public static AdCampaignResponse fromEntity(AdCampaign c) {
        return new AdCampaignResponse(
            c.getId(),
            c.getListing() != null ? c.getListing().getId() : null,
            c.getListing() != null ? c.getListing().getTitle() : null,
            c.getListing() != null && c.getListing().getCategory() != null ? c.getListing().getCategory().name() : null,
            c.getListing() != null ? c.getListing().getCity() : null,
            c.getClientUser() != null ? c.getClientUser().getId() : null,
            c.getClientUser() != null ? c.getClientUser().getName() : null,
            c.getTitle(),
            c.getHeadline(),
            c.getTargetType(),
            c.getTargetLat(),
            c.getTargetLng(),
            c.getRadiusKm(),
            c.getTargetCity(),
            c.getStatus(),
            c.getRejectionReason(),
            c.getStartDate(),
            c.getEndDate(),
            c.getImpressionsCount(),
            c.getClicksCount(),
            c.getCreatedAt(),
            c.getUpdatedAt()
        );
    }
}
