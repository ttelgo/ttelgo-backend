package com.tiktel.ttelgo.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating user profile information.
 * Contains userId in request body along with updatable fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    
    @NotNull(message = "userId is required")
    private Long userId;
    
    private String firstName;
    
    private String lastName;
    
    private String phone;
    
    @Email(message = "Email must be a valid email address")
    private String email;
}

