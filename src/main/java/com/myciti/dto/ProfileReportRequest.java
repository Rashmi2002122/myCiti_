package com.myciti.dto;

import com.myciti.model.ProfileReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProfileReportRequest {

    @NotNull(message = "Reason is required")
    private ProfileReportReason reason;

    @Size(max = 300, message = "Details cannot exceed 300 characters")
    private String details;

    public ProfileReportRequest() {}

    public ProfileReportRequest(ProfileReportReason reason, String details) {
        this.reason = reason;
        this.details = details;
    }

    public ProfileReportReason getReason() {
        return reason;
    }

    public void setReason(ProfileReportReason reason) {
        this.reason = reason;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
