package com.myciti.dto;

import com.myciti.model.Category;
import com.myciti.model.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public class ListingRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Category is required")
    private Category category;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Landmark is required")
    private String landmark;

    private String city = "Kolkata";

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Daily open time is required")
    private LocalTime dailyOpenTime;

    @NotNull(message = "Daily close time is required")
    private LocalTime dailyCloseTime;

    private String offerText;
    private String contactPhone;

    private Status status = Status.DRAFT;

    public ListingRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalTime getDailyOpenTime() { return dailyOpenTime; }
    public void setDailyOpenTime(LocalTime dailyOpenTime) { this.dailyOpenTime = dailyOpenTime; }

    public LocalTime getDailyCloseTime() { return dailyCloseTime; }
    public void setDailyCloseTime(LocalTime dailyCloseTime) { this.dailyCloseTime = dailyCloseTime; }

    public String getOfferText() { return offerText; }
    public void setOfferText(String offerText) { this.offerText = offerText; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
