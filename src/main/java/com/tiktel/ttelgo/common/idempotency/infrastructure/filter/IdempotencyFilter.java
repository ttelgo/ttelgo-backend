package com.tiktel.ttelgo.common.idempotency.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.idempotency.application.IdempotencyService;
import com.tiktel.ttelgo.common.idempotency.domain.IdempotencyRecord;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

/**
 * Filter to enforce idempotency for write operations (POST, PUT, PATCH, DELETE).
 * 
 * Behavior:
 * - Only processes requests to `/api/v1/**` with write methods
 * - Idempotency-Key header is OPTIONAL (backward compatible)
 * - If header present: check cache → replay if found, detect conflicts → 409 if payload differs
 * - If header missing: process normally (no idempotency enforcement)
 * - Actor ID extracted from: API key ID (request attribute) or user ID (auth context) or null (anonymous)
 * 
 * Filter order: Should run AFTER authentication filters so we can extract actor ID.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {
    
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final int IDEMPOTENCY_TTL_HOURS = 24;
    
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String method = request.getMethod();
        String requestPath = request.getRequestURI();
        
        // Only process write methods on /api/v1/** paths
        if (!WRITE_METHODS.contains(method) || !requestPath.startsWith("/api/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Check for Idempotency-Key header (optional - backward compatible)
        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            // No idempotency key - process normally
            filterChain.doFilter(request, response);
            return;
        }
        
        idempotencyKey = idempotencyKey.trim();
        
        // Extract actor ID (API key ID or user ID)
        Long actorId = extractActorId(request);
        
        // Wrap request to read body multiple times
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        String requestBody = getRequestBody(wrappedRequest);
        
        // Check for cached response
        IdempotencyService.IdempotencyResult cachedResult = idempotencyService.getCachedResponse(
                idempotencyKey, method, requestPath, actorId, requestBody
        ).orElse(IdempotencyService.IdempotencyResult.newRequest());
        
        if (cachedResult.isConflict()) {
            // Same key, different payload - conflict
            log.warn("Idempotency conflict: key={}, path={}, actor={}", idempotencyKey, requestPath, actorId);
            sendConflictResponse(response, "Idempotency-Key already used with a different request body");
            return;
        }
        
        if (cachedResult.isCached()) {
            // Replay cached response
            log.debug("Replaying cached response for idempotency key: {}", idempotencyKey);
            replayCachedResponse(response, cachedResult.getResponseStatus(), cachedResult.getResponseBody());
            return;
        }
        
        // New request - create pending record and process
        Optional<IdempotencyRecord> pendingRecordOpt = idempotencyService.createPendingRecord(
                idempotencyKey, method, requestPath, actorId, requestBody, IDEMPOTENCY_TTL_HOURS
        );
        
        if (pendingRecordOpt.isEmpty()) {
            // Concurrent request detected (another request with same key is processing)
            log.warn("Concurrent idempotency request detected: key={}, path={}", idempotencyKey, requestPath);
            sendConflictResponse(response, "Request with this Idempotency-Key is already being processed");
            return;
        }
        
        IdempotencyRecord pendingRecord = pendingRecordOpt.get();
        
        // Wrap response to capture body
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        
        try {
            // Process request
            filterChain.doFilter(wrappedRequest, wrappedResponse);
            
            // Extract response
            int responseStatus = wrappedResponse.getStatus();
            String responseBody = getResponseBody(wrappedResponse);
            
            // Update idempotency record with response
            idempotencyService.updateRecordWithResponse(pendingRecord.getId(), responseStatus, responseBody);
            
            // Copy response to actual response
            wrappedResponse.copyBodyToResponse();
            
        } catch (Exception e) {
            // On exception, mark record as failed (if possible)
            log.error("Error processing idempotent request: key={}", idempotencyKey, e);
            try {
                idempotencyService.updateRecordWithResponse(pendingRecord.getId(), 500, 
                        "{\"success\":false,\"message\":\"Internal server error\"}");
            } catch (Exception updateException) {
                log.warn("Failed to update idempotency record on error", updateException);
            }
            throw e;
        }
    }
    
    /**
     * Extract actor ID from request (API key ID or user ID from auth context).
     */
    private Long extractActorId(HttpServletRequest request) {
        // Try API key ID first (set by ApiKeyAuthenticationFilter)
        Object apiKeyIdAttr = request.getAttribute("apiKeyId");
        if (apiKeyIdAttr instanceof Long) {
            return (Long) apiKeyIdAttr;
        }
        
        // Try user ID from Spring Security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String) {
            // For JWT, principal might be user ID or email - would need to decode JWT
            // For now, we'll return null for user auth (can be enhanced later)
        }
        
        return null; // Anonymous request
    }
    
    /**
     * Read request body from cached request wrapper.
     */
    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }
        return new String(content, StandardCharsets.UTF_8);
    }
    
    /**
     * Read response body from cached response wrapper.
     */
    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }
        return new String(content, StandardCharsets.UTF_8);
    }
    
    /**
     * Replay a cached response.
     */
    private void replayCachedResponse(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(body);
        response.getWriter().flush();
    }
    
    /**
     * Send 409 Conflict response for idempotency key conflicts.
     */
    private void sendConflictResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        ApiResponse<Object> errorResponse = ApiResponse.error(message);
        String json = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(json);
        response.getWriter().flush();
    }
}

