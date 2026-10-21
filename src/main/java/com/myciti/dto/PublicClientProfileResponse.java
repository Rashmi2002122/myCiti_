package com.myciti.dto;

import com.myciti.model.ClientProfile;
import com.myciti.model.ClientType;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class PublicClientProfileResponse {

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
    private String publicPhone; // only present if showPhone is true
    private boolean showPhone;
    private String whatsappNumber;
    private LocalTime dailyOpenTime;
    private LocalTime dailyCloseTime;
    private boolean openNow;
    private boolean verified;
    private Instant verifiedAt;
    private Instant createdAt;
    private long activeListingsCount;
    private long upcomingListingsCount;

    public PublicClientProfileResponse() {
    }

    public static PublicClientProfileResponse fromEntity(ClientProfile profile, ZoneId zoneId, long activeCount, long upcomingCount) {
        if (profile == null) return null;

        PublicClientProfileResponse dto = new PublicClientProfileResponse();
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

        // STRICT PRIVACY: publicPhone is ONLY populated if showPhone is true
        dto.setShowPhone(profile.isShowPhone());
        if (profile.isShowPhone()) {
            dto.setPublicPhone(profile.getPublicPhone());
        } else {
            dto.setPublicPhone(null);
        }

        dto.setWhatsappNumber(profile.getWhatsappNumber());
        dto.setDailyOpenTime(profile.getDailyOpenTime());
        dto.setDailyCloseTime(profile.getDailyCloseTime());

        // Compute openNow
        dto.setOpenNow(computeOpenNow(profile.getDailyOpenTime(), profile.getDailyCloseTime(), zoneId));

        dto.setVerified(profile.isVerified());
        dto.setVerifiedAt(profile.getVerifiedAt());
        dto.setCreatedAt(profile.getCreatedAt());
        dto.setActiveListingsCount(activeCount);
        dto.setUpcomingListingsCount(upcomingCount);

        return dto;
    }

    private static boolean computeOpenNow(LocalTime open, LocalTime close, ZoneId zoneId) {
        if (open == null || close == null) return false;
        ZoneId activeZone = (zoneId != null) ? zoneId : ZoneId.of("Asia/Kolkata");
        LocalTime now = ZonedDateTime.now(activeZone).toLocalTime();

        if (open.isBefore(close)) {
            // Normal operating hours: e.g. 09:00 to 21:00
            return !now.isBefore(open) && now.isBefore(close);
        } else {
            // Overnight operating hours: e.g. 18:00 to 02:00
            return !now.isBefore(open) || now.isBefore(close);
        }
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

    public boolean isOpenNow() {
        return openNow;
    }

    public void setOpenNow(boolean openNow) {
        this.openNow = openNow;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
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
