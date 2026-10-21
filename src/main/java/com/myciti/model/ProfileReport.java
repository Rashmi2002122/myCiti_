package com.myciti.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "profile_reports")
public class ProfileReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private ClientProfile profile;

    @Column(name = "reporter_user_id")
    private Long reporterUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProfileReportReason reason;

    @Column(length = 300)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProfileReportStatus status = ProfileReportStatus.OPEN;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ProfileReport() {
    }

    public ProfileReport(ClientProfile profile, Long reporterUserId, ProfileReportReason reason, String details) {
        this.profile = profile;
        this.reporterUserId = reporterUserId;
        this.reason = reason;
        this.details = details;
        this.status = ProfileReportStatus.OPEN;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ClientProfile getProfile() {
        return profile;
    }

    public void setProfile(ClientProfile profile) {
        this.profile = profile;
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

    public Long getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(Long resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
