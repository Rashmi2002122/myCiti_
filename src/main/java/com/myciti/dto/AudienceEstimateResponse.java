package com.myciti.dto;

import com.myciti.model.TargetType;

public record AudienceEstimateResponse(
    TargetType targetType,
    Double radiusKm,
    String targetCity,
    String estimatedRange,
    boolean meetsPrivacyThreshold
) {}
