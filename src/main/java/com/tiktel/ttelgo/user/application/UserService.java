package com.tiktel.ttelgo.user.application;

import com.tiktel.ttelgo.common.exception.ResourceNotFoundException;
import com.tiktel.ttelgo.common.exception.ErrorCode;
import com.tiktel.ttelgo.common.exception.BusinessException;
import com.tiktel.ttelgo.user.api.dto.UpdateUserRequest;
import com.tiktel.ttelgo.user.api.dto.UpdateProfileRequest;
import com.tiktel.ttelgo.user.api.dto.UserResponse;
import com.tiktel.ttelgo.user.application.port.UserRepositoryPort;
import com.tiktel.ttelgo.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepositoryPort userRepositoryPort;
    
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.info("Getting user by ID: {}", id);
        
        User user = userRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND,
                        "User not found with ID: " + id));
        
        return toUserResponse(user);
    }
    
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        log.info("Getting user by email: {}", email);
        
        User user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND,
                        "User not found with email: " + email));
        
        return toUserResponse(user);
    }
    
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        log.info("Updating user: id={}", id);
        
        User user = userRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND,
                        "User not found with ID: " + id));
        
        // Update fields
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
        
        User savedUser = userRepositoryPort.save(user);
        log.info("User updated: id={}", savedUser.getId());
        
        return toUserResponse(savedUser);
    }
    
    /**
     * Update user profile information.
     * Validates that user exists before updating.
     * Updates firstName, lastName, phone, and email.
     * Automatically updates updatedAt timestamp via @PreUpdate.
     * 
     * @param request UpdateProfileRequest containing userId and fields to update
     * @return Updated user profile information
     */
    @Transactional
    public UserResponse updateUserProfile(UpdateProfileRequest request) {
        log.info("Updating user profile: userId={}", request.getUserId());
        
        // Validate that user exists
        User user = userRepositoryPort.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND,
                        "User not found with ID: " + request.getUserId()));
        
        // Update firstName
        if (request.getFirstName() != null && !request.getFirstName().trim().isEmpty()) {
            user.setFirstName(request.getFirstName().trim());
            log.debug("Updated firstName for user: {}", request.getUserId());
        }
        
        // Update lastName
        if (request.getLastName() != null && !request.getLastName().trim().isEmpty()) {
            user.setLastName(request.getLastName().trim());
            log.debug("Updated lastName for user: {}", request.getUserId());
        }
        
        // Update phone
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            String phone = request.getPhone().trim();
            // Check if phone is already used by another user
            userRepositoryPort.findByPhone(phone)
                    .ifPresent(existingUser -> {
                        if (!existingUser.getId().equals(request.getUserId())) {
                            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS,
                                    "Phone number already in use by another user");
                        }
                    });
            user.setPhone(phone);
            log.debug("Updated phone for user: {}", request.getUserId());
        }
        
        // Update email
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String email = request.getEmail().trim().toLowerCase();
            // Check if email is already used by another user
            userRepositoryPort.findByEmailIgnoreCase(email)
                    .ifPresent(existingUser -> {
                        if (!existingUser.getId().equals(request.getUserId())) {
                            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS,
                                    "Email already in use by another user");
                        }
                    });
            user.setEmail(email);
            log.debug("Updated email for user: {}", request.getUserId());
        }
        
        // Save user (updatedAt will be automatically updated via @PreUpdate)
        try {
            User savedUser = userRepositoryPort.save(user);
            log.info("User profile updated successfully: id={}, email={}", savedUser.getId(), savedUser.getEmail());
            return toUserResponse(savedUser);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while updating user profile: userId={}, error={}", 
                    request.getUserId(), e.getMessage());
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS,
                    "Email or phone number already in use by another user");
        }
    }
    
    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
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
                .role(user.getRole() != null ? user.getRole().name() : "USER")
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

