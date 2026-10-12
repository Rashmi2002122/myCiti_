package com.myciti.dto;

public class SubmissionImageDto {
    private Long id;
    private String url;
    private Integer sortOrder;

    public SubmissionImageDto() {
    }

    public SubmissionImageDto(Long id, String url, Integer sortOrder) {
        this.id = id;
        this.url = url;
        this.sortOrder = sortOrder;
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
}
