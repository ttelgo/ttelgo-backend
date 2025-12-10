package com.tiktel.ttelgo.user.api.mapper;

import com.tiktel.ttelgo.user.api.dto.UpdateUserRequest;
import com.tiktel.ttelgo.user.api.dto.UserResponse;
import com.tiktel.ttelgo.user.domain.User;
import org.springframework.stereotype.Component;

@Component
public class UserApiMapper {
    
    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .country(user.getCountry())
                .city(user.getCity())
                .address(user.getAddress())
                .postalCode(user.getPostalCode())
                .isEmailVerified(user.getIsEmailVerified())
                .isPhoneVerified(user.getIsPhoneVerified())
                .referralCode(user.getReferralCode())
                .referredBy(user.getReferredBy())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
    
    public void updateUserFromRequest(User user, UpdateUserRequest request) {
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getCountry() != null) {
            user.setCountry(request.getCountry());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getPostalCode() != null) {
            user.setPostalCode(request.getPostalCode());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
    }
}

