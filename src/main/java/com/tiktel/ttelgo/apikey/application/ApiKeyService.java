package com.tiktel.ttelgo.apikey.application;

import com.tiktel.ttelgo.apikey.api.dto.*;
import com.tiktel.ttelgo.apikey.api.mapper.ApiKeyMapper;
import com.tiktel.ttelgo.apikey.domain.ApiKey;
import com.tiktel.ttelgo.apikey.domain.ApiUsageLog;
import com.tiktel.ttelgo.apikey.infrastructure.repository.ApiKeyRepository;
import com.tiktel.ttelgo.apikey.infrastructure.repository.ApiUsageLogRepository;
import com.tiktel.ttelgo.common.exception.ErrorCode;
import com.tiktel.ttelgo.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApiKeyService {
    
    private final ApiKeyRepository apiKeyRepository;
    private final ApiUsageLogRepository usageLogRepository;
    private final ApiKeyMapper apiKeyMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Transactional
    public ApiKeyDto createApiKey(CreateApiKeyRequest request) {
        ApiKey apiKey = apiKeyMapper.toEntity(request);
        
        // Generate API key and secret
        String generatedKey = generateApiKey();
        String generatedSecret = generateApiSecret();
        
        apiKey.setApiKey(generatedKey);
        apiKey.setApiSecret(generatedSecret);
        
        ApiKey saved = apiKeyRepository.save(apiKey);
        ApiKeyDto dto = apiKeyMapper.toDto(saved);
        dto.setApiKey(generatedKey); // Show key only on creation
        return dto;
    }
    
    public List<ApiKeyDto> getAllApiKeys() {
        return apiKeyRepository.findAll().stream()
            .map(this::enrichWithStats)
            .collect(Collectors.toList());
    }

    public Page<ApiKeyDto> getApiKeys(Pageable pageable) {
        return apiKeyRepository.findAll(pageable).map(this::enrichWithStats);
    }
    
    public ApiKeyDto getApiKeyById(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "API key not found with id: " + id));
        return enrichWithStats(apiKey);
    }
    
    public ApiKeyDto getApiKeyByKey(String key) {
        ApiKey apiKey = apiKeyRepository.findByApiKey(key)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "API key not found"));
        return enrichWithStats(apiKey);
    }
    
    @Transactional
    public ApiKeyDto updateApiKey(Long id, UpdateApiKeyRequest request) {
        ApiKey apiKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "API key not found with id: " + id));
        
        apiKeyMapper.updateEntity(apiKey, request);
        ApiKey updated = apiKeyRepository.save(apiKey);
        return enrichWithStats(updated);
    }
    
    @Transactional
    public void deleteApiKey(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "API key not found with id: " + id));
        apiKeyRepository.delete(apiKey);
    }
    
    @Transactional
    public ApiKeyDto regenerateApiKey(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "API key not found with id: " + id));
        
        String newKey = generateApiKey();
        String newSecret = generateApiSecret();
        
        apiKey.setApiKey(newKey);
        apiKey.setApiSecret(newSecret);
        apiKey.setLastUsedAt(null);
        
        ApiKey updated = apiKeyRepository.save(apiKey);
        ApiKeyDto dto = apiKeyMapper.toDto(updated);
        dto.setApiKey(newKey); // Show new key only on regeneration
        return dto;
    }
    
    public ApiKey validateApiKey(String apiKey) {
        return apiKeyRepository.findByApiKey(apiKey)
            .filter(ApiKey::isValid)
            .orElseThrow(() -> new RuntimeException("Invalid or expired API key"));
    }
    
    @Transactional
    public void updateLastUsed(String apiKey) {
        apiKeyRepository.findByApiKey(apiKey).ifPresent(key -> {
            key.setLastUsedAt(LocalDateTime.now());
            apiKeyRepository.save(key);
        });
    }
    
    public ApiUsageStatsDto getUsageStats(Long apiKeyId, Integer days) {
        if (days == null || days <= 0) days = 30;
        LocalDateTime start = LocalDateTime.now().minusDays(days);
        
        List<ApiUsageLog> logs = usageLogRepository.findByApiKeyIdAndCreatedAtBetween(
            apiKeyId, start, LocalDateTime.now());
        
        long totalRequests = logs.size();
        long requestsToday = usageLogRepository.countByApiKeyIdSince(
            apiKeyId, LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));
        long requestsThisWeek = usageLogRepository.countByApiKeyIdSince(
            apiKeyId, LocalDateTime.now().minusWeeks(1));
        long requestsThisMonth = usageLogRepository.countByApiKeyIdSince(
            apiKeyId, LocalDateTime.now().minusMonths(1));
        
        Double avgResponseTime = usageLogRepository.getAverageResponseTime(apiKeyId, start);
        long totalErrors = logs.stream()
            .filter(log -> log.getStatusCode() != null && log.getStatusCode() >= 400)
            .count();
        
        double errorRate = totalRequests > 0 ? (double) totalErrors / totalRequests * 100 : 0;
        
        List<Object[]> topEndpoints = usageLogRepository.getTopEndpointsByApiKeyId(apiKeyId, start);
        List<Object[]> statusCodes = usageLogRepository.getStatusCodesByApiKeyId(apiKeyId, start);
        List<Object[]> dailyUsage = usageLogRepository.getDailyUsageStats(apiKeyId, start);
        
        return ApiUsageStatsDto.builder()
            .totalRequests(totalRequests)
            .requestsToday(requestsToday)
            .requestsThisWeek(requestsThisWeek)
            .requestsThisMonth(requestsThisMonth)
            .averageResponseTime(avgResponseTime != null ? avgResponseTime : 0.0)
            .totalErrors(totalErrors)
            .errorRate(errorRate)
            .topEndpoints(topEndpoints.stream()
                .limit(10)
                .map(arr -> ApiUsageStatsDto.EndpointUsageDto.builder()
                    .endpoint((String) arr[0])
                    .count((Long) arr[1])
                    .build())
                .collect(Collectors.toList()))
            .statusCodeDistribution(statusCodes.stream()
                .map(arr -> ApiUsageStatsDto.StatusCodeCountDto.builder()
                    .statusCode((Integer) arr[0])
                    .count((Long) arr[1])
                    .build())
                .collect(Collectors.toList()))
            .dailyUsage(dailyUsage.stream()
                .map(arr -> ApiUsageStatsDto.DailyUsageDto.builder()
                    .date(arr[0].toString())
                    .requests((Long) arr[1])
                    .build())
                .collect(Collectors.toList()))
            .build();
    }
    
    private ApiKeyDto enrichWithStats(ApiKey apiKey) {
        ApiKeyDto dto = apiKeyMapper.toDto(apiKey);
        
        // Preserve apiKey field if it was set (for creation/regeneration)
        // Don't overwrite it if it's already set in the DTO
        
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        dto.setTotalRequests(usageLogRepository.countByApiKeyIdSince(apiKey.getId(), 
            apiKey.getCreatedAt() != null ? apiKey.getCreatedAt() : LocalDateTime.now().minusYears(1)));
        dto.setRequestsToday(usageLogRepository.countByApiKeyIdSince(apiKey.getId(), todayStart));
        
        Double avgResponseTime = usageLogRepository.getAverageResponseTime(apiKey.getId(), 
            LocalDateTime.now().minusDays(30));
        dto.setAverageResponseTime(avgResponseTime != null ? avgResponseTime : 0.0);
        
        return dto;
    }
    
    private String generateApiKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String key = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return "ttelgo_" + key;
    }
    
    private String generateApiSecret() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

