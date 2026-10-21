package com.myciti.dto;

import com.myciti.model.ClientType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/**
 * Request payload for PUT /api/me/client-profile.
 * Clients cannot edit slug, verified status, or profile status.
 */
public class UpdateClientProfileRequest {

    @Size(max = 120, message = "Business name cannot exceed 120 characters")
    private String businessName;

    private ClientType clientType;

    @Size(max = 160, message = "Bio cannot exceed 160 characters")
    private String bio;

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String address;

    @Size(max = 255, message = "Landmark cannot exceed 255 characters")
    private String landmark;

    private Double latitude;

    private Double longitude;

    @Pattern(
        regexp = "^$|^(\\+91[\\-\\s]?|0)?[6-9]\\d{9}$",
        message = "Public phone must be a valid 10-digit Indian mobile number"
    )
    private String publicPhone;

    private Boolean showPhone;

    @Pattern(
        regexp = "^$|^(\\+91[\\-\\s]?|0)?[6-9]\\d{9}$",
        message = "WhatsApp number must be a valid 10-digit Indian mobile number"
    )
    private String whatsappNumber;

    private LocalTime dailyOpenTime;

    private LocalTime dailyCloseTime;

    public UpdateClientProfileRequest() {}

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

    public Boolean getShowPhone() {
        return showPhone;
    }

    public void setShowPhone(Boolean showPhone) {
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
}
