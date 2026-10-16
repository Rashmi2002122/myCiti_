package com.myciti.dto;

import jakarta.validation.constraints.Size;

public class UpdateLocationRequest {

    private Double latitude;
    private Double longitude;

    @Size(max = 100, message = "Area name cannot exceed 100 characters")
    private String areaName;

    public UpdateLocationRequest() {}

    public UpdateLocationRequest(Double latitude, Double longitude, String areaName) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.areaName = areaName;
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

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }
}
