package com.myciti.dto;

import com.myciti.model.TargetType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class AdCampaignRequest {

    @NotNull(message = "Listing ID is required")
    private Long listingId;

    @NotBlank(message = "Campaign title is required")
    @Size(max = 150, message = "Campaign title cannot exceed 150 characters")
    private String title;

    @Size(max = 255, message = "Headline cannot exceed 255 characters")
    private String headline;

    @NotNull(message = "Target type is required")
    private TargetType targetType = TargetType.RADIUS;

    private Double targetLat;
    private Double targetLng;

    @DecimalMin(value = "0.5", message = "Radius must be at least 0.5 km")
    @DecimalMax(value = "25.0", message = "Radius cannot exceed 25.0 km")
    private Double radiusKm = 5.0;

    @Size(max = 100, message = "Target city cannot exceed 100 characters")
    private String targetCity;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    public AdCampaignRequest() {}

    public Long getListingId() {
        return listingId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
    }

    public Double getTargetLat() {
        return targetLat;
    }

    public void setTargetLat(Double targetLat) {
        this.targetLat = targetLat;
    }

    public Double getTargetLng() {
        return targetLng;
    }

    public void setTargetLng(Double targetLng) {
        this.targetLng = targetLng;
    }

    public Double getRadiusKm() {
        return radiusKm;
    }

    public void setRadiusKm(Double radiusKm) {
        this.radiusKm = radiusKm;
    }

    public String getTargetCity() {
        return targetCity;
    }

    public void setTargetCity(String targetCity) {
        this.targetCity = targetCity;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
