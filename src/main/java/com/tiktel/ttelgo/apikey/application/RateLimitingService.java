package com.tiktel.ttelgo.apikey.application;

import com.tiktel.ttelgo.apikey.domain.ApiKey;
import com.tiktel.ttelgo.apikey.domain.ApiRateLimitTracking;
import com.tiktel.ttelgo.apikey.infrastructure.repository.ApiRateLimitTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitingService {
    
    private final ApiRateLimitTrackingRepository rateLimitRepository;
    
    /**
     * Check if the API key has exceeded its rate limits
     * @return true if rate limit is exceeded, false otherwise
     */
    @Transactional
    public boolean isRateLimitExceeded(ApiKey apiKey) {
        LocalDateTime now = LocalDateTime.now();
        
        // Check per-minute limit
        LocalDateTime minuteStart = now.truncatedTo(ChronoUnit.MINUTES);
        if (checkRateLimit(apiKey.getId(), "minute", minuteStart, apiKey.getRateLimitPerMinute())) {
            log.warn("Rate limit exceeded: API key {} exceeded per-minute limit of {}", 
                    apiKey.getId(), apiKey.getRateLimitPerMinute());
            return true;
        }
        
        // Check per-hour limit
        LocalDateTime hourStart = now.truncatedTo(ChronoUnit.HOURS);
        if (checkRateLimit(apiKey.getId(), "hour", hourStart, apiKey.getRateLimitPerHour())) {
            log.warn("Rate limit exceeded: API key {} exceeded per-hour limit of {}", 
                    apiKey.getId(), apiKey.getRateLimitPerHour());
            return true;
        }
        
        // Check per-day limit
        LocalDateTime dayStart = now.truncatedTo(ChronoUnit.DAYS);
        if (checkRateLimit(apiKey.getId(), "day", dayStart, apiKey.getRateLimitPerDay())) {
            log.warn("Rate limit exceeded: API key {} exceeded per-day limit of {}", 
                    apiKey.getId(), apiKey.getRateLimitPerDay());
            return true;
        }
        
        return false;
    }
    
    /**
     * Increment the request count for the given time window
     */
    @Transactional
    public void incrementRequestCount(Long apiKeyId, String windowType, LocalDateTime windowStart) {
        Optional<ApiRateLimitTracking> tracking = rateLimitRepository
            .findByApiKeyIdAndWindowTypeAndWindowStart(apiKeyId, windowType, windowStart);
        
        if (tracking.isPresent()) {
            ApiRateLimitTracking track = tracking.get();
            track.setRequestCount(track.getRequestCount() + 1);
            track.setUpdatedAt(LocalDateTime.now());
            rateLimitRepository.save(track);
        } else {
            ApiRateLimitTracking newTracking = ApiRateLimitTracking.builder()
                .apiKeyId(apiKeyId)
                .windowType(windowType)
                .windowStart(windowStart)
                .requestCount(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            rateLimitRepository.save(newTracking);
        }
    }
    
    private boolean checkRateLimit(Long apiKeyId, String windowType, LocalDateTime windowStart, Integer limit) {
        if (limit == null || limit <= 0) {
            return false; // No limit set
        }
        
        Optional<ApiRateLimitTracking> tracking = rateLimitRepository
            .findByApiKeyIdAndWindowTypeAndWindowStart(apiKeyId, windowType, windowStart);
        
        if (tracking.isPresent()) {
            return tracking.get().getRequestCount() >= limit;
        }
        
        return false; // No requests in this window yet
    }
    
    /**
     * Get remaining requests for a time window
     */
    public int getRemainingRequests(ApiKey apiKey, String windowType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart;
        Integer limit;
        
        switch (windowType) {
            case "minute":
                windowStart = now.truncatedTo(ChronoUnit.MINUTES);
                limit = apiKey.getRateLimitPerMinute();
                break;
            case "hour":
                windowStart = now.truncatedTo(ChronoUnit.HOURS);
                limit = apiKey.getRateLimitPerHour();
                break;
            case "day":
                windowStart = now.truncatedTo(ChronoUnit.DAYS);
                limit = apiKey.getRateLimitPerDay();
                break;
            default:
                return -1;
        }
        
        if (limit == null || limit <= 0) {
            return Integer.MAX_VALUE;
        }
        
        Optional<ApiRateLimitTracking> tracking = rateLimitRepository
            .findByApiKeyIdAndWindowTypeAndWindowStart(apiKey.getId(), windowType, windowStart);
        
        int currentCount = tracking.map(ApiRateLimitTracking::getRequestCount).orElse(0);
        return Math.max(0, limit - currentCount);
    }
}

