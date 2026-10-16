package com.myciti.dto;

import java.time.Instant;

public record InAppNotificationDto(
    Long id,
    String title,
    String message,
    String linkUrl,
    boolean read,
    Instant createdAt
) {}
