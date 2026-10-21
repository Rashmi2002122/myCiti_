package com.myciti.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_sub", nullable = false, unique = true)
    private String googleSub;

    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    @Column(name = "picture_url", length = 1000)
    private String pictureUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", length = 50)
    private ClientType clientType;

    @Column(name = "business_name")
    private String businessName;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "profile_complete", nullable = false)
    private boolean profileComplete = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_login_at", nullable = false)
    private Instant lastLoginAt = Instant.now();

    @Column(name = "coarse_lat")
    private Double coarseLat;

    @Column(name = "coarse_lng")
    private Double coarseLng;

    @Column(name = "area_name", length = 100)
    private String areaName;

    @Column(name = "location_updated_at")
    private Instant locationUpdatedAt;

    public User() {}

    public User(String googleSub, String email, String name, String pictureUrl) {
        this.googleSub = googleSub;
        this.email = email;
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.role = Role.USER;
        this.status = UserStatus.ACTIVE;
        this.profileComplete = false;
        this.createdAt = Instant.now();
        this.lastLoginAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public void setGoogleSub(String googleSub) {
        this.googleSub = googleSub;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }

    // Backward-compatible alias for accountType
    public AccountType getAccountType() {
        if (clientType == null) return null;
        try {
            return AccountType.valueOf(clientType.name());
        } catch (Exception e) {
            return null;
        }
    }

    public void setAccountType(AccountType accountType) {
        if (accountType == null) {
            this.clientType = null;
        } else {
            try {
                this.clientType = ClientType.valueOf(accountType.name());
            } catch (Exception e) {
                this.clientType = null;
            }
        }
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public boolean isProfileComplete() {
        return profileComplete;
    }

    public void setProfileComplete(boolean profileComplete) {
        this.profileComplete = profileComplete;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Double getCoarseLat() {
        return coarseLat;
    }

    public void setCoarseLat(Double coarseLat) {
        this.coarseLat = coarseLat;
    }

    public Double getCoarseLng() {
        return coarseLng;
    }

    public void setCoarseLng(Double coarseLng) {
        this.coarseLng = coarseLng;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public Instant getLocationUpdatedAt() {
        return locationUpdatedAt;
    }

    public void setLocationUpdatedAt(Instant locationUpdatedAt) {
        this.locationUpdatedAt = locationUpdatedAt;
    }
}
