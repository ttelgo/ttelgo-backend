package com.tiktel.ttelgo.user.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String country;
    private String city;
    private String address;
    private String postalCode;
    private Boolean isEmailVerified;
    private Boolean isPhoneVerified;
    private String referralCode;
    private Long referredBy;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

