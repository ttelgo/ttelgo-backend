package com.tiktel.ttelgo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiktel.ttelgo.apikey.application.ApiKeyService;
import com.tiktel.ttelgo.apikey.application.RateLimitingService;
import com.tiktel.ttelgo.apikey.domain.ApiKey;
import com.tiktel.ttelgo.apikey.domain.ApiUsageLog;
import com.tiktel.ttelgo.apikey.infrastructure.repository.ApiUsageLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    
    private final ApiKeyService apiKeyService;
    private final ApiUsageLogRepository usageLogRepository;
    private final RateLimitingService rateLimitingService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String API_SECRET_HEADER = "X-API-Secret";
    
    // Paths that don't require API key authentication (public endpoints for frontend)
    private static final List<String> EXEMPT_PATHS = List.of(
        "/api/auth/**",
        "/api/health/**",
        "/api/plans/**",  // Plans/bundles endpoints (public for frontend)
        "/api/faq/**",  // FAQ endpoints (public for frontend)
        "/api/blog/**",  // Blog endpoints (public for frontend)
        "/api/webhooks/stripe/**",
        "/api/admin/api-keys/**",  // Allow API key management without API key (for initial setup)
        "/api-docs/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/swagger-ui/index.html",
        "/actuator/**",
        "/error"
    );
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Check if path is exempted
        boolean isExempted = EXEMPT_PATHS.stream()
            .anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
        
        if (isExempted) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // All other paths require API key
        String apiKey = request.getHeader(API_KEY_HEADER);
        String apiSecret = request.getHeader(API_SECRET_HEADER);
        
        if (apiKey == null || apiKey.isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                "API key is required. Please provide X-API-Key header.");
            return;
        }
        
        try {
            ApiKey key = apiKeyService.validateApiKey(apiKey);
            
            // Validate secret if provided
            if (apiSecret != null && !apiSecret.isEmpty()) {
                if (!key.getApiSecret().equals(apiSecret)) {
                    logUsage(request, key.getId(), 401, "Invalid API secret", null);
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                        "Invalid API secret.");
                    return;
                }
            }
            
            // Check IP whitelist if configured
            if (key.getAllowedIps() != null && !key.getAllowedIps().isEmpty()) {
                String clientIp = getClientIpAddress(request);
                if (!isIpAllowed(key.getAllowedIps(), clientIp)) {
                    logUsage(request, key.getId(), 403, "IP not allowed: " + clientIp, null);
                    sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, 
                        "IP address not allowed: " + clientIp);
                    return;
                }
            }
            
            // Check scopes if configured
            if (key.getScopes() != null && !key.getScopes().isEmpty()) {
                if (!isScopeAllowed(key.getScopes(), requestPath, request.getMethod())) {
                    logUsage(request, key.getId(), 403, "Scope not allowed for endpoint: " + requestPath, null);
                    sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, 
                        "API key does not have permission to access this endpoint.");
                    return;
                }
            }
            
            // Check rate limits
            if (rateLimitingService.isRateLimitExceeded(key)) {
                logUsage(request, key.getId(), 429, "Rate limit exceeded", null);
                sendErrorResponse(response, 429, 
                    "Rate limit exceeded. Please try again later.");
                return;
            }
            
            // Increment rate limit counters
            LocalDateTime now = LocalDateTime.now();
            rateLimitingService.incrementRequestCount(key.getId(), "minute", now.truncatedTo(ChronoUnit.MINUTES));
            rateLimitingService.incrementRequestCount(key.getId(), "hour", now.truncatedTo(ChronoUnit.HOURS));
            rateLimitingService.incrementRequestCount(key.getId(), "day", now.truncatedTo(ChronoUnit.DAYS));
            
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
            
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("API key authentication failed", e);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                "Invalid or expired API key.");
        }
    }
    
    private boolean isScopeAllowed(String scopesJson, String endpoint, String method) {
        try {
            if (scopesJson == null || scopesJson.isEmpty()) {
                return true; // No scopes means all allowed
            }
            
            // Parse JSON array of scopes
            List<String> scopes = objectMapper.readValue(scopesJson, 
                new TypeReference<List<String>>() {});
            
            if (scopes.isEmpty()) {
                return true; // Empty list means all allowed
            }
            
            // Check for wildcard
            if (scopes.contains("*") || scopes.stream().anyMatch(s -> s.contains("**"))) {
                return true;
            }
            
            // Check exact match: METHOD:/api/endpoint
            String methodAndPath = method + ":" + endpoint;
            if (scopes.contains(methodAndPath)) {
                return true;
            }
            
            // Check pattern matching (e.g., GET:/api/plans/** matches GET:/api/plans/123)
            for (String scope : scopes) {
                if (scope.contains(":")) {
                    String[] parts = scope.split(":", 2);
                    if (parts.length == 2) {
                        String scopeMethod = parts[0].trim();
                        String scopePath = parts[1].trim();
                        
                        // Check method match
                        if (scopeMethod.equals("*") || scopeMethod.equals(method)) {
                            // Check path pattern
                            if (pathMatcher.match(scopePath, endpoint) || 
                                scopePath.equals("*") || 
                                endpoint.startsWith(scopePath.replace("**", ""))) {
                                return true;
                            }
                        }
                    }
                } else {
                    // Just path pattern
                    if (pathMatcher.match(scope, endpoint) || scope.equals("*")) {
                        return true;
                    }
                }
            }
            
            return false;
        } catch (Exception e) {
            log.warn("Error checking scopes", e);
            return false; // Fail secure - deny access if scope check fails
        }
    }
    
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("status", status);
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
    }
    
    private boolean isIpAllowed(String allowedIpsJson, String clientIp) {
        try {
            if (allowedIpsJson == null || allowedIpsJson.isEmpty()) {
                return true; // No IP restrictions means all allowed
            }
            
            // Parse JSON array of IPs
            List<String> allowedIps = objectMapper.readValue(allowedIpsJson, 
                new TypeReference<List<String>>() {});
            
            if (allowedIps.isEmpty()) {
                return true; // Empty list means all allowed
            }
            
            // Check for wildcard
            if (allowedIps.contains("*")) {
                return true;
            }
            
            // Check exact match
            if (allowedIps.contains(clientIp)) {
                return true;
            }
            
            // Check CIDR notation (basic support - can be enhanced)
            for (String allowedIp : allowedIps) {
                if (allowedIp.contains("/")) {
                    // Basic CIDR check (simplified)
                    String[] parts = allowedIp.split("/");
                    if (parts.length == 2) {
                        String networkIp = parts[0].trim();
                        // For now, just check if it starts with the network IP
                        // Full CIDR implementation would require proper subnet calculation
                        if (clientIp.startsWith(networkIp.substring(0, networkIp.lastIndexOf('.')))) {
                            return true;
                        }
                    }
                } else if (allowedIp.equals(clientIp)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            log.warn("Error checking IP whitelist", e);
            return false; // Fail secure - deny access if IP check fails
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
            log.error("Failed to log API usage", e);
        }
    }
}

