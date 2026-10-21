package com.myciti.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserCountsDto {

    private Long savedCount;
    private Long pendingCount;
    private Long approvedCount;
    private Long declinedCount;
    private Long pendingReviewCount;
    private Long pendingClientRequestCount;

    public UserCountsDto() {}

    public UserCountsDto(Long savedCount) {
        this.savedCount = savedCount;
    }

    public Long getSavedCount() {
        return savedCount;
    }

    public void setSavedCount(Long savedCount) {
        this.savedCount = savedCount;
    }

    public Long getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(Long pendingCount) {
        this.pendingCount = pendingCount;
    }

    public Long getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(Long approvedCount) {
        this.approvedCount = approvedCount;
    }

    public Long getDeclinedCount() {
        return declinedCount;
    }

    public void setDeclinedCount(Long declinedCount) {
        this.declinedCount = declinedCount;
    }

    public Long getPendingReviewCount() {
        return pendingReviewCount;
    }

    public void setPendingReviewCount(Long pendingReviewCount) {
        this.pendingReviewCount = pendingReviewCount;
    }

    public Long getPendingClientRequestCount() {
        return pendingClientRequestCount;
    }

    public void setPendingClientRequestCount(Long pendingClientRequestCount) {
        this.pendingClientRequestCount = pendingClientRequestCount;
    }
}
