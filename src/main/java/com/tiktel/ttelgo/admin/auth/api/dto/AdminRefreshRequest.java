package com.tiktel.ttelgo.admin.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Admin token refresh request DTO.
 * Used to refresh expired access tokens using a valid refresh token.
 */
@Data
public class AdminRefreshRequest {
    
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}

