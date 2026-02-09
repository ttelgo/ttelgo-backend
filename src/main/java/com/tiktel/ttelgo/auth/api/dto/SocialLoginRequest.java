package com.tiktel.ttelgo.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified DTO for social login requests (Google, Facebook, Apple).
 * All social login endpoints accept the same request format:
 * {
 *   "idToken": "SOCIAL_PROVIDER_ID_TOKEN"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginRequest {
    
    @NotBlank(message = "idToken is required")
    private String idToken;
}
