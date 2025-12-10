package com.tiktel.ttelgo.apikey.api.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiktel.ttelgo.apikey.api.dto.ApiKeyDto;
import com.tiktel.ttelgo.apikey.api.dto.CreateApiKeyRequest;
import com.tiktel.ttelgo.apikey.api.dto.UpdateApiKeyRequest;
import com.tiktel.ttelgo.apikey.domain.ApiKey;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ApiKeyMapper {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public ApiKeyDto toDto(ApiKey apiKey) {
        if (apiKey == null) return null;
        
        try {
            List<String> allowedIps = apiKey.getAllowedIps() != null && !apiKey.getAllowedIps().isEmpty()
                ? objectMapper.readValue(apiKey.getAllowedIps(), new TypeReference<List<String>>() {})
                : new ArrayList<>();
            
            List<String> scopes = apiKey.getScopes() != null && !apiKey.getScopes().isEmpty()
                ? objectMapper.readValue(apiKey.getScopes(), new TypeReference<List<String>>() {})
                : new ArrayList<>();
            
            return ApiKeyDto.builder()
                .id(apiKey.getId())
                .keyName(apiKey.getKeyName())
                .apiKey(apiKey.getApiKey()) // Include API key in DTO
                .customerName(apiKey.getCustomerName())
                .customerEmail(apiKey.getCustomerEmail())
                .isActive(apiKey.getIsActive())
                .rateLimitPerMinute(apiKey.getRateLimitPerMinute())
                .rateLimitPerHour(apiKey.getRateLimitPerHour())
                .rateLimitPerDay(apiKey.getRateLimitPerDay())
                .allowedIps(allowedIps)
                .scopes(scopes)
                .expiresAt(apiKey.getExpiresAt())
                .lastUsedAt(apiKey.getLastUsedAt())
                .createdAt(apiKey.getCreatedAt())
                .updatedAt(apiKey.getUpdatedAt())
                .notes(apiKey.getNotes())
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Error mapping ApiKey to DTO", e);
        }
    }
    
    public ApiKey toEntity(CreateApiKeyRequest request) {
        if (request == null) return null;
        
        try {
            String allowedIpsJson = request.getAllowedIps() != null && !request.getAllowedIps().isEmpty()
                ? objectMapper.writeValueAsString(request.getAllowedIps())
                : null;
            
            String scopesJson = request.getScopes() != null && !request.getScopes().isEmpty()
                ? objectMapper.writeValueAsString(request.getScopes())
                : null;
            
            return ApiKey.builder()
                .keyName(request.getKeyName())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .rateLimitPerMinute(request.getRateLimitPerMinute())
                .rateLimitPerHour(request.getRateLimitPerHour())
                .rateLimitPerDay(request.getRateLimitPerDay())
                .allowedIps(allowedIpsJson)
                .scopes(scopesJson)
                .expiresAt(request.getExpiresAt())
                .notes(request.getNotes())
                .isActive(true)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Error mapping CreateApiKeyRequest to Entity", e);
        }
    }
    
    public void updateEntity(ApiKey apiKey, UpdateApiKeyRequest request) {
        if (apiKey == null || request == null) return;
        
        try {
            if (request.getKeyName() != null) {
                apiKey.setKeyName(request.getKeyName());
            }
            if (request.getCustomerName() != null) {
                apiKey.setCustomerName(request.getCustomerName());
            }
            if (request.getCustomerEmail() != null) {
                apiKey.setCustomerEmail(request.getCustomerEmail());
            }
            if (request.getIsActive() != null) {
                apiKey.setIsActive(request.getIsActive());
            }
            if (request.getRateLimitPerMinute() != null) {
                apiKey.setRateLimitPerMinute(request.getRateLimitPerMinute());
            }
            if (request.getRateLimitPerHour() != null) {
                apiKey.setRateLimitPerHour(request.getRateLimitPerHour());
            }
            if (request.getRateLimitPerDay() != null) {
                apiKey.setRateLimitPerDay(request.getRateLimitPerDay());
            }
            if (request.getAllowedIps() != null) {
                apiKey.setAllowedIps(request.getAllowedIps().isEmpty() 
                    ? null 
                    : objectMapper.writeValueAsString(request.getAllowedIps()));
            }
            if (request.getScopes() != null) {
                apiKey.setScopes(request.getScopes().isEmpty() 
                    ? null 
                    : objectMapper.writeValueAsString(request.getScopes()));
            }
            if (request.getExpiresAt() != null) {
                apiKey.setExpiresAt(request.getExpiresAt());
            }
            if (request.getNotes() != null) {
                apiKey.setNotes(request.getNotes());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error updating ApiKey entity", e);
        }
    }
}

