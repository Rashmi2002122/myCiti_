package com.myciti.dto;

import com.myciti.model.AccountType;
import com.myciti.model.ClientType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @Size(max = 255, message = "name cannot exceed 255 characters")
    private String name;

    private ClientType clientType;

    private AccountType accountType;

    @Size(max = 255, message = "businessName cannot exceed 255 characters")
    private String businessName;

    @Pattern(
        regexp = "^$|^(\\+91[\\-\\s]?)?[6-9]\\d{9}$",
        message = "phone must be a valid 10-digit Indian phone number (optionally prefixed with +91)"
    )
    private String phone;

    public UpdateProfileRequest() {}

    public UpdateProfileRequest(AccountType accountType, String businessName, String phone) {
        this.accountType = accountType;
        if (accountType != null) {
            try {
                this.clientType = ClientType.valueOf(accountType.name());
            } catch (Exception ignored) {}
        }
        this.businessName = businessName;
        this.phone = phone;
    }

    public UpdateProfileRequest(String name, ClientType clientType, String businessName, String phone) {
        this.name = name;
        this.clientType = clientType;
        if (clientType != null) {
            try {
                this.accountType = AccountType.valueOf(clientType.name());
            } catch (Exception ignored) {}
        }
        this.businessName = businessName;
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ClientType getClientType() {
        if (clientType != null) return clientType;
        if (accountType != null) {
            try {
                return ClientType.valueOf(accountType.name());
            } catch (Exception ignored) {}
        }
        return null;
    }

    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
        if (clientType != null) {
            try {
                this.accountType = AccountType.valueOf(clientType.name());
            } catch (Exception ignored) {}
        }
    }

    public AccountType getAccountType() {
        if (accountType != null) return accountType;
        if (clientType != null) {
            try {
                return AccountType.valueOf(clientType.name());
            } catch (Exception ignored) {}
        }
        return null;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
        if (accountType != null) {
            try {
                this.clientType = ClientType.valueOf(accountType.name());
            } catch (Exception ignored) {}
        }
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
