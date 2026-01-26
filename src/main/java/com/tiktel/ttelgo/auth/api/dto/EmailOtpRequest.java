package com.tiktel.ttelgo.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailOtpRequest {
    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    private String email;
}










