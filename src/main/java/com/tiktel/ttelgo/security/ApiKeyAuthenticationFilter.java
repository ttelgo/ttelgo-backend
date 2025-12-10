package com.tiktel.ttelgo.security;

import com.tiktel.ttelgo.apikey.application.ApiKeyService;
import com.tiktel.ttelgo.apikey.domain.ApiKey;
import com.tiktel.ttelgo.apikey.domain.ApiUsageLog;
import com.tiktel.ttelgo.apikey.infrastructure.repository.ApiUsageLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    
    private final ApiKeyService apiKeyService;
    private final ApiUsageLogRepository usageLogRepository;
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String API_SECRET_HEADER = "X-API-Secret";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String apiKey = request.getHeader(API_KEY_HEADER);
        String apiSecret = request.getHeader(API_SECRET_HEADER);
        
        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                ApiKey key = apiKeyService.validateApiKey(apiKey);
                
                // Validate secret if provided
                if (apiSecret != null && !apiSecret.isEmpty()) {
                    if (!key.getApiSecret().equals(apiSecret)) {
                        logUsage(request, key.getId(), 401, "Invalid API secret", null);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                }
                
                // Check IP whitelist if configured
                if (key.getAllowedIps() != null && !key.getAllowedIps().isEmpty()) {
                    String clientIp = getClientIpAddress(request);
                    if (!isIpAllowed(key.getAllowedIps(), clientIp)) {
                        logUsage(request, key.getId(), 403, "IP not allowed: " + clientIp, null);
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                }
                
                // Check rate limits (simplified - can be enhanced)
                // This is a basic check, full rate limiting should be done in a separate filter
                
                // Set authentication
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        key.getApiKey(),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_KEY"))
                    );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // Update last used
                apiKeyService.updateLastUsed(apiKey);
                
                // Log usage after successful authentication
                request.setAttribute("apiKeyId", key.getId());
                
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean isIpAllowed(String allowedIpsJson, String clientIp) {
        try {
            // Simple check - can be enhanced with proper JSON parsing
            return allowedIpsJson.contains(clientIp) || allowedIpsJson.contains("*");
        } catch (Exception e) {
            return false;
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
    
    private void logUsage(HttpServletRequest request, Long apiKeyId, int statusCode, 
                         String errorMessage, Long responseTime) {
        try {
            ApiUsageLog log = ApiUsageLog.builder()
                .apiKeyId(apiKeyId)
                .endpoint(request.getRequestURI())
                .method(request.getMethod())
                .statusCode(statusCode)
                .responseTimeMs(responseTime != null ? responseTime.intValue() : null)
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .errorMessage(errorMessage)
                .createdAt(LocalDateTime.now())
                .build();
            
            usageLogRepository.save(log);
        } catch (Exception e) {
            // Log error but don't fail the request
            logger.error("Failed to log API usage", e);
        }
    }
}

