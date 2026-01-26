package com.tiktel.ttelgo.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    @JsonProperty("token")
    private String accessToken;  // JWT access token (use this in Authorization: Bearer header)
    private String refreshToken; // JWT refresh token (use for token refresh)
    private String tokenType;    // Always "Bearer"
    private Long expiresIn;      // Token expiration time in milliseconds
    private UserDto user;
    
    // Legacy method for backward compatibility - ignored by Jackson to avoid conflict
    @Deprecated
    @JsonIgnore
    public String getToken() {
        return accessToken;
    }
    
    @Deprecated
    @JsonIgnore
    public void setToken(String token) {
        this.accessToken = token;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private Long id;
        private String email;
        @JsonProperty("fullName")
        private String name; // Full name (mapped to fullName in JSON response)
        private String phone;
        private String firstName;
        private String lastName;
        private Boolean isEmailVerified;
        private Boolean isPhoneVerified;
        private String role; // USER, ADMIN, SUPER_ADMIN
    }
}

