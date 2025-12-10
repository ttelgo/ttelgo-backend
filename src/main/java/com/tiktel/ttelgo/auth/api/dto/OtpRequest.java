package com.tiktel.ttelgo.auth.api.dto;

import lombok.Data;

@Data
public class OtpRequest {
    private String email;
    private String phone;
    private String purpose; // LOGIN, REGISTER, RESET_PASSWORD, VERIFY_EMAIL, VERIFY_PHONE
}

