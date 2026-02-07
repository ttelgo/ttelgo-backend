package com.tiktel.ttelgo.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpVerifyRequest {
    /**
     * Email address (optional - either email or phone must be provided).
     * Validated in service layer for format and presence.
     */
    private String email;
    
    /**
     * Phone number in E.164 format (optional - either email or phone must be provided).
     * Validated in service layer for format and presence.
     */
    private String phone;
    
    @NotBlank(message = "OTP is required")
    private String otp;
    
    /**
     * Custom validation: At least one of email or phone must be provided.
     * This is validated in the service layer.
     */
    public boolean isValid() {
        return (email != null && !email.trim().isEmpty()) || 
               (phone != null && !phone.trim().isEmpty());
    }
}

