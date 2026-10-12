package com.myciti.dto;

import com.myciti.model.Category;
import com.myciti.model.Submission;
import com.myciti.model.SubmissionStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SubmissionResponse {

    private Long id;
    private Long userId;
    private String submitterName;
    private String submitterEmail;
    private String submitterPhone;
    private String submitterAccountType;
    private Category category;
    private String title;
    private String description;
    private String venueName;
    private String address;
    private String landmark;
    private String city;
    private Double latitude;
    private Double longitude;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime dailyOpenTime;
    private LocalTime dailyCloseTime;
    private String offerText;
    private String contactPhone;
    private SubmissionStatus status;
    private String declineReason;
    private String reviewedBy;
    private Instant reviewedAt;
    private Long listingId;
    private List<SubmissionImageDto> images = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public SubmissionResponse() {
    }

    public static SubmissionResponse fromEntity(Submission s) {
        SubmissionResponse res = new SubmissionResponse();
        res.setId(s.getId());
        if (s.getUser() != null) {
            res.setUserId(s.getUser().getId());
            res.setSubmitterName(s.getUser().getName());
            res.setSubmitterEmail(s.getUser().getEmail());
            res.setSubmitterPhone(s.getUser().getPhone());
            if (s.getUser().getAccountType() != null) {
                res.setSubmitterAccountType(s.getUser().getAccountType().name());
            }
        }
        res.setCategory(s.getCategory());
        res.setTitle(s.getTitle());
        res.setDescription(s.getDescription());
        res.setVenueName(s.getVenueName());
        res.setAddress(s.getAddress());
        res.setLandmark(s.getLandmark());
        res.setCity(s.getCity());
        res.setLatitude(s.getLatitude());
        res.setLongitude(s.getLongitude());
        res.setStartDate(s.getStartDate());
        res.setEndDate(s.getEndDate());
        res.setDailyOpenTime(s.getDailyOpenTime());
        res.setDailyCloseTime(s.getDailyCloseTime());
        res.setOfferText(s.getOfferText());
        res.setContactPhone(s.getContactPhone());
        res.setStatus(s.getStatus());
        res.setDeclineReason(s.getDeclineReason());
        res.setReviewedBy(s.getReviewedBy());
        res.setReviewedAt(s.getReviewedAt());
        res.setListingId(s.getListingId());
        res.setCreatedAt(s.getCreatedAt());
        res.setUpdatedAt(s.getUpdatedAt());

        if (s.getImages() != null) {
            res.setImages(s.getImages().stream()
                    .map(img -> new SubmissionImageDto(img.getId(), img.getUrl(), img.getSortOrder()))
                    .collect(Collectors.toList()));
        }
        return res;
    }

    // Getters and Setters
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

    public String getSubmitterName() {
        return submitterName;
    }

    public void setSubmitterName(String submitterName) {
        this.submitterName = submitterName;
    }

    public String getSubmitterEmail() {
        return submitterEmail;
    }

    public void setSubmitterEmail(String submitterEmail) {
        this.submitterEmail = submitterEmail;
    }

    public String getSubmitterPhone() {
        return submitterPhone;
    }

    public void setSubmitterPhone(String submitterPhone) {
        this.submitterPhone = submitterPhone;
    }

    public String getSubmitterAccountType() {
        return submitterAccountType;
    }

    public void setSubmitterAccountType(String submitterAccountType) {
        this.submitterAccountType = submitterAccountType;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLandmark() {
        return landmark;
    }

    public void setLandmark(String landmark) {
        this.landmark = landmark;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalTime getDailyOpenTime() {
        return dailyOpenTime;
    }

    public void setDailyOpenTime(LocalTime dailyOpenTime) {
        this.dailyOpenTime = dailyOpenTime;
    }

    public LocalTime getDailyCloseTime() {
        return dailyCloseTime;
    }

    public void setDailyCloseTime(LocalTime dailyCloseTime) {
        this.dailyCloseTime = dailyCloseTime;
    }

    public String getOfferText() {
        return offerText;
    }

    public void setOfferText(String offerText) {
        this.offerText = offerText;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(SubmissionStatus status) {
        this.status = status;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public void setDeclineReason(String declineReason) {
        this.declineReason = declineReason;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Long getListingId() {
        return listingId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }

    public List<SubmissionImageDto> getImages() {
        return images;
    }

    public void setImages(List<SubmissionImageDto> images) {
        this.images = images;
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
