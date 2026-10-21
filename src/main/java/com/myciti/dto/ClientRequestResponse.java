package com.myciti.dto;

import com.myciti.model.ClientRequest;
import com.myciti.model.ClientRequestStatus;
import com.myciti.model.ClientType;

import java.time.Instant;

public class ClientRequestResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userPictureUrl;
    private ClientType clientType;
    private String businessName;
    private String contactPhone;
    private String note;
    private ClientRequestStatus status;
    private String declineReason;
    private Long reviewedBy;
    private Instant reviewedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public ClientRequestResponse() {}

    public static ClientRequestResponse fromEntity(ClientRequest req) {
        if (req == null) return null;
        ClientRequestResponse dto = new ClientRequestResponse();
        dto.setId(req.getId());
        if (req.getUser() != null) {
            dto.setUserId(req.getUser().getId());
            dto.setUserName(req.getUser().getName());
            dto.setUserEmail(req.getUser().getEmail());
            dto.setUserPictureUrl(req.getUser().getPictureUrl());
        }
        dto.setClientType(req.getClientType());
        dto.setBusinessName(req.getBusinessName());
        dto.setContactPhone(req.getContactPhone());
        dto.setNote(req.getNote());
        dto.setStatus(req.getStatus());
        dto.setDeclineReason(req.getDeclineReason());
        if (req.getReviewedBy() != null) {
            dto.setReviewedBy(req.getReviewedBy().getId());
        }
        dto.setReviewedAt(req.getReviewedAt());
        dto.setCreatedAt(req.getCreatedAt());
        dto.setUpdatedAt(req.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserPictureUrl() {
        return userPictureUrl;
    }

    public void setUserPictureUrl(String userPictureUrl) {
        this.userPictureUrl = userPictureUrl;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public ClientRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ClientRequestStatus status) {
        this.status = status;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public void setDeclineReason(String declineReason) {
        this.declineReason = declineReason;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
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
}
