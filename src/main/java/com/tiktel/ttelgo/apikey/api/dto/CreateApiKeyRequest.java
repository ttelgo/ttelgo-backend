package com.tiktel.ttelgo.apikey.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApiKeyRequest {
    
    @NotBlank(message = "Key name is required")
    private String keyName;
    
    @NotBlank(message = "Customer name is required")
    private String customerName;
    
    @Email(message = "Valid email is required")
    @NotBlank(message = "Customer email is required")
    private String customerEmail;
    
    @Min(value = 1, message = "Rate limit per minute must be at least 1")
    @Builder.Default
    private Integer rateLimitPerMinute = 60;
    
    @Min(value = 1, message = "Rate limit per hour must be at least 1")
    @Builder.Default
    private Integer rateLimitPerHour = 1000;
    
    @Min(value = 1, message = "Rate limit per day must be at least 1")
    @Builder.Default
    private Integer rateLimitPerDay = 10000;
    
    private List<String> allowedIps;
    
    private List<String> scopes;
    
    private LocalDateTime expiresAt;
    
    private String notes;
}

