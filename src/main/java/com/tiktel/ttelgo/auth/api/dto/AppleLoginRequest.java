package com.tiktel.ttelgo.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppleLoginRequest {
    @NotBlank(message = "identityToken is required")
    private String identityToken;
}










