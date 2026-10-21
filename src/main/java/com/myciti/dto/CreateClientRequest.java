package com.myciti.dto;

import com.myciti.model.ClientType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateClientRequest {

    @NotNull(message = "clientType is required")
    private ClientType clientType;

    @Size(max = 255, message = "businessName cannot exceed 255 characters")
    private String businessName;

    @NotNull(message = "contactPhone is required")
    @Pattern(
        regexp = "^(\\+91[\\-\\s]?)?[6-9]\\d{9}$",
        message = "contactPhone must be a valid 10-digit Indian phone number (optionally prefixed with +91)"
    )
    private String contactPhone;

    @Size(max = 1000, message = "note cannot exceed 1000 characters")
    private String note;

    public CreateClientRequest() {}

    public CreateClientRequest(ClientType clientType, String businessName, String contactPhone, String note) {
        this.clientType = clientType;
        this.businessName = businessName;
        this.contactPhone = contactPhone;
        this.note = note;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
