package com.tiktel.ttelgo.admin.auth.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Admin authentication response DTO.
 * Contains JWT tokens and user information for authenticated admin users.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuthResponse {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn; // in seconds
    private Long refreshExpiresIn; // in seconds
    
    private AdminUserInfo user;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminUserInfo {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private String name;
        private String role;
        private String userType;
        private List<String> scopes;
        private Boolean isEmailVerified;
    }
}

