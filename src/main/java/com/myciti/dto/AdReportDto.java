package com.myciti.dto;

import com.myciti.model.AdReport;

import java.time.Instant;

public record AdReportDto(
    Long id,
    Long campaignId,
    String campaignTitle,
    Long reporterUserId,
    String reporterEmail,
    String reason,
    String details,
    Instant createdAt
) {
    public static AdReportDto fromEntity(AdReport r) {
        return new AdReportDto(
            r.getId(),
            r.getCampaign() != null ? r.getCampaign().getId() : null,
            r.getCampaign() != null ? r.getCampaign().getTitle() : null,
            r.getReporterUser() != null ? r.getReporterUser().getId() : null,
            r.getReporterUser() != null ? r.getReporterUser().getEmail() : null,
            r.getReason(),
            r.getDetails(),
            r.getCreatedAt()
        );
    }
}
