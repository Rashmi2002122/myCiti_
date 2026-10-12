package com.myciti.dto;

import com.myciti.model.AccountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @NotNull(message = "accountType is required")
    private AccountType accountType;

    @Size(max = 255, message = "businessName cannot exceed 255 characters")
    private String businessName;

    @NotNull(message = "phone is required")
    @Pattern(
        regexp = "^(\\+91[\\-\\s]?)?[6-9]\\d{9}$",
        message = "phone must be a valid 10-digit Indian phone number (optionally prefixed with +91)"
    )
    private String phone;

    public UpdateProfileRequest() {}

    public UpdateProfileRequest(AccountType accountType, String businessName, String phone) {
        this.accountType = accountType;
        this.businessName = businessName;
        this.phone = phone;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
