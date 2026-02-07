package com.tiktel.ttelgo.security;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Rate limiting filter using Resilience4j
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private final RateLimiterRegistry rateLimiterRegistry;
    
    public RateLimitingFilter() {
        // Create rate limiter registry with default config
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(100)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofMillis(100))
                .build();
        
        this.rateLimiterRegistry = RateLimiterRegistry.of(config);
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String clientId = getClientIdentifier(request);
        String endpoint = request.getRequestURI();
        
        // Skip rate limiting for health checks and webhooks
        if (endpoint.startsWith("/actuator/") || endpoint.startsWith("/api/v1/webhooks/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(clientId);
        
        boolean allowed = rateLimiter.acquirePermission();
        
        if (!allowed) {
            log.warn("Rate limit exceeded for client: {}, endpoint: {}", clientId, endpoint);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Rate limit exceeded. Please try again later.\"}");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getClientIdentifier(HttpServletRequest request) {
        // Try to get API key from header
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null) {
            return "api-key:" + apiKey.substring(0, Math.min(10, apiKey.length()));
        }
        
        // Try to get user from JWT (simplified - needs proper JWT extraction)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return "user:jwt";
        }
        
        // Fall back to IP address
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return "ip:" + ipAddress;
    }
}

