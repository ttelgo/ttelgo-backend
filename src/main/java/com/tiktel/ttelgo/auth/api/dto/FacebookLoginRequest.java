package com.tiktel.ttelgo.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Facebook login request.
 * Contains the Facebook access token (idToken).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacebookLoginRequest {
    
    @NotBlank(message = "idToken is required")
    private String idToken;
}

