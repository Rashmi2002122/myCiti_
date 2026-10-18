package com.myciti.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public class UpdateLocationRequest {

    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
    private Double longitude;

    @Size(max = 100, message = "City name cannot exceed 100 characters")
    private String city;

    public UpdateLocationRequest() {}

    public UpdateLocationRequest(Double latitude, Double longitude, String city) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.city = city;
    }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
}
