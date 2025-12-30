package com.tiktel.ttelgo.apikey.api.dto;

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
public class ApiKeyDto {
    private Long id;
    private String keyName;
    private String apiKey; // Only shown on creation
    private String customerName;
    private String customerEmail;
    private Boolean isActive;
    private Integer rateLimitPerMinute;
    private Integer rateLimitPerHour;
    private Integer rateLimitPerDay;
    private List<String> allowedIps;
    private List<String> scopes;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String notes;
    private Long totalRequests;
    private Long requestsToday;
    private Double averageResponseTime;
}

