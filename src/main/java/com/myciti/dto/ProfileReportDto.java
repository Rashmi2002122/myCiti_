package com.myciti.dto;

import com.myciti.model.ProfileReport;
import com.myciti.model.ProfileReportReason;
import com.myciti.model.ProfileReportStatus;

import java.time.Instant;

public class ProfileReportDto {
    private Long id;
    private Long profileId;
    private String clientSlug;
    private String clientBusinessName;
    private Long reporterUserId;
    private ProfileReportReason reason;
    private String details;
    private ProfileReportStatus status;
    private Instant createdAt;

    public ProfileReportDto() {}

    public static ProfileReportDto fromEntity(ProfileReport report) {
        if (report == null) return null;
        ProfileReportDto dto = new ProfileReportDto();
        dto.setId(report.getId());
        if (report.getProfile() != null) {
            dto.setProfileId(report.getProfile().getId());
            dto.setClientSlug(report.getProfile().getSlug());
            dto.setClientBusinessName(report.getProfile().getBusinessName());
        }
        dto.setReporterUserId(report.getReporterUserId());
        dto.setReason(report.getReason());
        dto.setDetails(report.getDetails());
        dto.setStatus(report.getStatus());
        dto.setCreatedAt(report.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
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

    public Long getReporterUserId() {
        return reporterUserId;
    }

    public void setReporterUserId(Long reporterUserId) {
        this.reporterUserId = reporterUserId;
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

    public ProfileReportStatus getStatus() {
        return status;
    }

    public void setStatus(ProfileReportStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
