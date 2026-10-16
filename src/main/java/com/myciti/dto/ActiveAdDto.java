package com.myciti.dto;

public record ActiveAdDto(
    Long campaignId,
    Long listingId,
    String title,
    String headline,
    String category,
    String address,
    String landmark,
    String city,
    Double latitude,
    Double longitude,
    String offerText,
    String contactPhone,
    String primaryImageUrl,
    Double distanceKm,
    boolean sponsored
) {}
