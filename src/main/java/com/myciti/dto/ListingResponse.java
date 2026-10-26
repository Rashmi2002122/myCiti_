package com.myciti.dto;

import com.myciti.model.Category;
import com.myciti.model.Status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ListingResponse {
    private Long id;
    private String title;
    private Category category;
    private String description;
    private String address;
    private String landmark;
    private String city;
    private Double latitude;
    private Double longitude;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime dailyOpenTime;
    private LocalTime dailyCloseTime;
    private String offerText;
    private String contactPhone;
    private Status status;
    private boolean openNow;
    private Boolean isExpired;
    private Double distanceKm;
    private String primaryImageUrl;
    private List<ImageResponse> images;
    private Long clientProfileId;
    private String clientSlug;
    private String clientBusinessName;
    private String clientLogoUrl;
    private Boolean clientVerified;
    private String clientType;
    private Boolean hasOtherUpdates;
    private Integer otherUpdatesCount;
    private Instant createdAt;
    private Instant updatedAt;

    public ListingResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLandmark() {
        return landmark;
    }

    public void setLandmark(String landmark) {
        this.landmark = landmark;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
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

    public LocalTime getDailyOpenTime() {
        return dailyOpenTime;
    }

    public void setDailyOpenTime(LocalTime dailyOpenTime) {
        this.dailyOpenTime = dailyOpenTime;
    }

    public LocalTime getDailyCloseTime() {
        return dailyCloseTime;
    }

    public void setDailyCloseTime(LocalTime dailyCloseTime) {
        this.dailyCloseTime = dailyCloseTime;
    }

    public String getOfferText() {
        return offerText;
    }

    public void setOfferText(String offerText) {
        this.offerText = offerText;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isOpenNow() {
        return openNow;
    }

    public void setOpenNow(boolean openNow) {
        this.openNow = openNow;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public String getPrimaryImageUrl() {
        return primaryImageUrl;
    }

    public void setPrimaryImageUrl(String primaryImageUrl) {
        this.primaryImageUrl = primaryImageUrl;
    }

    public List<ImageResponse> getImages() {
        return images;
    }

    public void setImages(List<ImageResponse> images) {
        this.images = images;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getClientProfileId() {
        return clientProfileId;
    }

    public void setClientProfileId(Long clientProfileId) {
        this.clientProfileId = clientProfileId;
    }

    public String getClientSlug() {
        return clientSlug;
    }

    public void setClientSlug(String clientSlug) {
        this.clientSlug = clientSlug;
    }

    public String getClientBusinessName() {
        return clientBusinessName;
    }

    public void setClientBusinessName(String clientBusinessName) {
        this.clientBusinessName = clientBusinessName;
    }

    public String getClientLogoUrl() {
        return clientLogoUrl;
    }

    public void setClientLogoUrl(String clientLogoUrl) {
        this.clientLogoUrl = clientLogoUrl;
    }

    public Boolean getClientVerified() {
        return clientVerified;
    }

    public void setClientVerified(Boolean clientVerified) {
        this.clientVerified = clientVerified;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public Boolean getHasOtherUpdates() {
        return hasOtherUpdates;
    }

    public void setHasOtherUpdates(Boolean hasOtherUpdates) {
        this.hasOtherUpdates = hasOtherUpdates;
    }

    public Integer getOtherUpdatesCount() {
        return otherUpdatesCount;
    }

    public void setOtherUpdatesCount(Integer otherUpdatesCount) {
        this.otherUpdatesCount = otherUpdatesCount;
    }

    public Boolean getIsExpired() {
        return isExpired;
    }

    public void setIsExpired(Boolean isExpired) {
        this.isExpired = isExpired;
    }
}
