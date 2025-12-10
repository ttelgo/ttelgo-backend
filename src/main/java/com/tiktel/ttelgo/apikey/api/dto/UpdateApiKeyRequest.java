package com.tiktel.ttelgo.apikey.api.dto;

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
public class UpdateApiKeyRequest {
    private String keyName;
    private String customerName;
    private String customerEmail;
    private Boolean isActive;
    
    @Min(value = 1, message = "Rate limit per minute must be at least 1")
    private Integer rateLimitPerMinute;
    
    @Min(value = 1, message = "Rate limit per hour must be at least 1")
    private Integer rateLimitPerHour;
    
    @Min(value = 1, message = "Rate limit per day must be at least 1")
    private Integer rateLimitPerDay;
    
    private List<String> allowedIps;
    private List<String> scopes;
    private LocalDateTime expiresAt;
    private String notes;
}

