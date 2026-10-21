package com.myciti.dto;

import java.time.Instant;

public class ImageResponse {
    private Long id;
    private String url;
    private Integer sortOrder;
    private Instant createdAt;

    public ImageResponse() {
    }

    public ImageResponse(Long id, String url, Integer sortOrder) {
        this(id, url, sortOrder, null);
    }

    public ImageResponse(Long id, String url, Integer sortOrder, Instant createdAt) {
        this.id = id;
        this.url = url;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
