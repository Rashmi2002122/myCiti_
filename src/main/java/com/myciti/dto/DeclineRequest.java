package com.myciti.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DeclineRequest {

    @NotBlank(message = "Decline reason is required")
    @Size(min = 10, message = "Decline reason must be at least 10 characters")
    private String reason;

    public DeclineRequest() {
    }

    public DeclineRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
