package com.myciti.dto;

import com.myciti.model.ClientProfile;
import com.myciti.model.ClientProfileStatus;
import com.myciti.model.ClientType;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Client private profile view (for GET /api/me/client-profile).
 * Contains full profile configuration (including publicPhone even when showPhone is false),
 * while never leaking login email, Google sub, user ID, or internal admin notes.
 */
public class ClientProfileResponse {

    private Long id;
    private String slug;
    private String businessName;
    private ClientType clientType;
    private String bio;
    private String logoUrl;
    private String coverUrl;
    private String address;
    private String landmark;
    private Double latitude;
    private Double longitude;
    private String publicPhone;
    private boolean showPhone;
    private String whatsappNumber;
    private LocalTime dailyOpenTime;
    private LocalTime dailyCloseTime;
    private boolean verified;
    private Instant verifiedAt;
    private ClientProfileStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean openNow;
    private long activeListingsCount;
    private long upcomingListingsCount;

    public ClientProfileResponse() {}

    public static ClientProfileResponse fromEntity(ClientProfile profile, ZoneId zoneId, long activeCount, long upcomingCount) {
        if (profile == null) return null;
        ClientProfileResponse dto = new ClientProfileResponse();
        dto.setId(profile.getId());
        dto.setSlug(profile.getSlug());
        dto.setBusinessName(profile.getBusinessName());
        dto.setClientType(profile.getClientType());
        dto.setBio(profile.getBio());
        dto.setLogoUrl(profile.getLogoUrl());
        dto.setCoverUrl(profile.getCoverUrl());
        dto.setAddress(profile.getAddress());
        dto.setLandmark(profile.getLandmark());
        dto.setLatitude(profile.getLatitude());
        dto.setLongitude(profile.getLongitude());
        dto.setPublicPhone(profile.getPublicPhone());
        dto.setShowPhone(profile.isShowPhone());
        dto.setWhatsappNumber(profile.getWhatsappNumber());
        dto.setDailyOpenTime(profile.getDailyOpenTime());
        dto.setDailyCloseTime(profile.getDailyCloseTime());
        dto.setVerified(profile.isVerified());
        dto.setVerifiedAt(profile.getVerifiedAt());
        dto.setStatus(profile.getStatus());
        dto.setCreatedAt(profile.getCreatedAt());
        dto.setUpdatedAt(profile.getUpdatedAt());
        dto.setActiveListingsCount(activeCount);
        dto.setUpcomingListingsCount(upcomingCount);

        // Calculate openNow
        if (profile.getDailyOpenTime() != null && profile.getDailyCloseTime() != null) {
            LocalTime nowTime = ZonedDateTime.now(zoneId).toLocalTime();
            LocalTime open = profile.getDailyOpenTime();
            LocalTime close = profile.getDailyCloseTime();
            if (open.isBefore(close)) {
                dto.setOpenNow(!nowTime.isBefore(open) && nowTime.isBefore(close));
            } else {
                // Overnight hours
                dto.setOpenNow(!nowTime.isBefore(open) || nowTime.isBefore(close));
            }
        } else {
            dto.setOpenNow(null);
        }

        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
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

    public String getPublicPhone() {
        return publicPhone;
    }

    public void setPublicPhone(String publicPhone) {
        this.publicPhone = publicPhone;
    }

    public boolean isShowPhone() {
        return showPhone;
    }

    public void setShowPhone(boolean showPhone) {
        this.showPhone = showPhone;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public void setWhatsappNumber(String whatsappNumber) {
        this.whatsappNumber = whatsappNumber;
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

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public ClientProfileStatus getStatus() {
        return status;
    }

    public void setStatus(ClientProfileStatus status) {
        this.status = status;
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

    public Boolean getOpenNow() {
        return openNow;
    }

    public void setOpenNow(Boolean openNow) {
        this.openNow = openNow;
    }

    public long getActiveListingsCount() {
        return activeListingsCount;
    }

    public void setActiveListingsCount(long activeListingsCount) {
        this.activeListingsCount = activeListingsCount;
    }

    public long getUpcomingListingsCount() {
        return upcomingListingsCount;
    }

    public void setUpcomingListingsCount(long upcomingListingsCount) {
        this.upcomingListingsCount = upcomingListingsCount;
    }
}
