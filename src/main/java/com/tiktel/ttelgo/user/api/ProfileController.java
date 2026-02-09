package com.tiktel.ttelgo.user.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.user.api.dto.UpdateProfileRequest;
import com.tiktel.ttelgo.user.api.dto.UserResponse;
import com.tiktel.ttelgo.user.application.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for user profile operations.
 * Provides endpoints for fetching and updating user profile information.
 */
@RestController
@RequestMapping("/api/user/profile")
@Slf4j
public class ProfileController {
    
    private final UserService userService;
    
    @Autowired
    public ProfileController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Fetch logged-in user profile by userId.
     * 
     * GET /api/user/profile/{userId}
     * 
     * @param userId The ID of the user whose profile to fetch
     * @return User profile information
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(@PathVariable Long userId) {
        log.info("Fetching user profile for userId: {}", userId);
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Update user profile information.
     * 
     * PUT /api/user/profile
     * 
     * Updates the following fields:
     * - firstName
     * - lastName
     * - phone
     * - email
     * 
     * Validates that user exists before updating.
     * Automatically updates updatedAt timestamp.
     * 
     * @param request UpdateProfileRequest containing userId and fields to update
     * @return Updated user profile information
     */
    @PutMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateUserProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        log.info("Updating user profile for userId: {}", request.getUserId());
        UserResponse response = userService.updateUserProfile(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

