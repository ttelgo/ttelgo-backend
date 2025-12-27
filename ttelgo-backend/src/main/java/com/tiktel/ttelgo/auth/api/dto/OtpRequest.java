package com.tiktel.ttelgo.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OtpRequest {
    
    @Email(message = "Invalid email format")
    private String email;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format (must be E.164 format)")
    private String phone;
    
    @Pattern(regexp = "^(LOGIN|REGISTER|RESET_PASSWORD|VERIFY_EMAIL|VERIFY_PHONE)$", 
            message = "Purpose must be one of: LOGIN, REGISTER, RESET_PASSWORD, VERIFY_EMAIL, VERIFY_PHONE")
    private String purpose; // LOGIN, REGISTER, RESET_PASSWORD, VERIFY_EMAIL, VERIFY_PHONE
    
    // Note: At least one of email or phone should be provided, but this is handled in service layer
}

