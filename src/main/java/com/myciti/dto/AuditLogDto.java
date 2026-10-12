package com.myciti.dto;

import java.time.Instant;

public class AuditLogDto {
    private Long id;
    private Long actorUserId;
    private String action;
    private String details;
    private Instant createdAt;

    public AuditLogDto() {}

    public AuditLogDto(Long id, Long actorUserId, String action, String details, Instant createdAt) {
        this.id = id;
        this.actorUserId = actorUserId;
        this.action = action;
        this.details = details;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
