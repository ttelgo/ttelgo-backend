package com.tiktel.ttelgo.auth.infrastructure.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for caching OTP tokens in Redis for faster lookup and expiry management.
 * This is an optional enhancement - PostgreSQL remains the source of truth.
 * 
 * Benefits:
 * - Faster OTP verification (Redis lookup is faster than DB query)
 * - Automatic expiry via Redis TTL
 * - Reduced database load for high-traffic scenarios
 */
@Service
@Slf4j
public class OtpCacheService {
    
    private static final String OTP_CACHE_PREFIX = "otp:";
    private static final String OTP_ATTEMPTS_PREFIX = "otp:attempts:";
    private static final int OTP_CACHE_TTL_MINUTES = 5; // Match OTP expiry
    
    private final StringRedisTemplate redisTemplate;
    
    @Autowired
    public OtpCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Cache OTP token ID for fast lookup by email or phone.
     * Key format: "otp:email:{email}" or "otp:phone:{phone}"
     * Value: OTP token ID (for database lookup)
     * TTL: 5 minutes (matches OTP expiry)
     */
    public void cacheOtpTokenId(String email, String phone, Long tokenId) {
        try {
            if (email != null && !email.trim().isEmpty()) {
                String key = OTP_CACHE_PREFIX + "email:" + email.toLowerCase().trim();
                redisTemplate.opsForValue().set(key, String.valueOf(tokenId), OTP_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                log.debug("Cached OTP token ID {} for email: {}", tokenId, email);
            }
            if (phone != null && !phone.trim().isEmpty()) {
                String key = OTP_CACHE_PREFIX + "phone:" + phone.trim();
                redisTemplate.opsForValue().set(key, String.valueOf(tokenId), OTP_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                log.debug("Cached OTP token ID {} for phone: {}", tokenId, phone);
            }
        } catch (Exception e) {
            log.warn("Failed to cache OTP token in Redis: {}", e.getMessage());
            // Don't throw - Redis is optional, PostgreSQL is source of truth
        }
    }
    
    /**
     * Get cached OTP token ID from Redis.
     * Returns null if not found or Redis unavailable.
     */
    public Long getCachedOtpTokenId(String email, String phone) {
        try {
            if (email != null && !email.trim().isEmpty()) {
                String key = OTP_CACHE_PREFIX + "email:" + email.toLowerCase().trim();
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    return Long.parseLong(value);
                }
            }
            if (phone != null && !phone.trim().isEmpty()) {
                String key = OTP_CACHE_PREFIX + "phone:" + phone.trim();
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    return Long.parseLong(value);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get cached OTP token from Redis: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Invalidate cached OTP token (delete from Redis).
     * Called when OTP is used or invalidated.
     */
    public void invalidateCachedOtp(String email, String phone) {
        try {
            if (email != null && !email.trim().isEmpty()) {
                String key = OTP_CACHE_PREFIX + "email:" + email.toLowerCase().trim();
                redisTemplate.delete(key);
                log.debug("Invalidated cached OTP for email: {}", email);
            }
            if (phone != null && !phone.trim().isEmpty()) {
                String key = OTP_CACHE_PREFIX + "phone:" + phone.trim();
                redisTemplate.delete(key);
                log.debug("Invalidated cached OTP for phone: {}", phone);
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate cached OTP in Redis: {}", e.getMessage());
        }
    }
    
    /**
     * Track OTP verification attempts in Redis.
     * Key format: "otp:attempts:{email}" or "otp:attempts:{phone}"
     * Value: Current attempt count
     * TTL: 5 minutes (matches OTP expiry)
     */
    public void incrementAttempts(String email, String phone) {
        try {
            if (email != null && !email.trim().isEmpty()) {
                String key = OTP_ATTEMPTS_PREFIX + "email:" + email.toLowerCase().trim();
                Long attempts = redisTemplate.opsForValue().increment(key);
                redisTemplate.expire(key, OTP_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                log.debug("Incremented OTP attempts for email {}: {}", email, attempts);
            }
            if (phone != null && !phone.trim().isEmpty()) {
                String key = OTP_ATTEMPTS_PREFIX + "phone:" + phone.trim();
                Long attempts = redisTemplate.opsForValue().increment(key);
                redisTemplate.expire(key, OTP_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                log.debug("Incremented OTP attempts for phone {}: {}", phone, attempts);
            }
        } catch (Exception e) {
            log.warn("Failed to increment OTP attempts in Redis: {}", e.getMessage());
        }
    }
    
    /**
     * Get current attempt count from Redis.
     * Returns 0 if not found or Redis unavailable.
     */
    public int getAttempts(String email, String phone) {
        try {
            if (email != null && !email.trim().isEmpty()) {
                String key = OTP_ATTEMPTS_PREFIX + "email:" + email.toLowerCase().trim();
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    return Integer.parseInt(value);
                }
            }
            if (phone != null && !phone.trim().isEmpty()) {
                String key = OTP_ATTEMPTS_PREFIX + "phone:" + phone.trim();
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    return Integer.parseInt(value);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get OTP attempts from Redis: {}", e.getMessage());
        }
        return 0;
    }
    
    /**
     * Reset attempt count (when OTP is verified successfully).
     */
    public void resetAttempts(String email, String phone) {
        try {
            if (email != null && !email.trim().isEmpty()) {
                String key = OTP_ATTEMPTS_PREFIX + "email:" + email.toLowerCase().trim();
                redisTemplate.delete(key);
            }
            if (phone != null && !phone.trim().isEmpty()) {
                String key = OTP_ATTEMPTS_PREFIX + "phone:" + phone.trim();
                redisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.warn("Failed to reset OTP attempts in Redis: {}", e.getMessage());
        }
    }
}

