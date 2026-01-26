package com.tiktel.ttelgo.apikey.security;

import com.tiktel.ttelgo.apikey.domain.ApiKey;
import com.tiktel.ttelgo.apikey.infrastructure.repository.ApiKeyRepository;
import com.tiktel.ttelgo.security.ApiKeyAuthenticationDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * API Client Resolver - extracts API client information from authentication context.
 * Used to get current API client details (vendor, user, etc.) from authenticated requests.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiClientResolver {
    
    private final ApiKeyRepository apiKeyRepository;
    
    /**
     * Get current API client (ApiKey) from authentication context.
     * Returns the ApiKey entity for the currently authenticated API client.
     * 
     * @return ApiKey entity or empty if not authenticated as API client
     */
    public Optional<ApiKey> getCurrentApiClient() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        
        // Check if authenticated as API client (ROLE_API_KEY)
        if (!authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_API_KEY"))) {
            return Optional.empty();
        }
        
        // Try to get API key ID from authentication details (preferred method)
        if (authentication.getDetails() instanceof ApiKeyAuthenticationDetails) {
            ApiKeyAuthenticationDetails details = (ApiKeyAuthenticationDetails) authentication.getDetails();
            Long apiKeyId = details.getApiKeyId();
            if (apiKeyId != null) {
                return apiKeyRepository.findById(apiKeyId);
            }
        }
        
        // Fallback: Get API key string from authentication principal
        String apiKeyString = authentication.getName();
        
        try {
            // Find API key - since keys are hashed, we need to find by comparing
            // Note: This is optimized in the filter where we already found the key
            // Here we can use a simpler lookup if we store a reference
            // For now, return empty and let caller handle
            log.debug("Getting API client for key: {}", 
                    apiKeyString.length() > 20 ? apiKeyString.substring(0, 20) + "..." : apiKeyString);
            
            // The authentication principal contains the hashed key string
            // In practice, we should store the key ID in authentication details
            // For now, return empty - actual implementation should store key ID
            return Optional.empty();
            
        } catch (Exception e) {
            log.warn("Failed to resolve API client", e);
            return Optional.empty();
        }
    }
    
    /**
     * Get current API client ID from authentication context.
     * 
     * @return API key ID or null if not authenticated as API client
     */
    public Long getCurrentApiClientId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        // Check if authenticated as API client
        if (!authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_API_KEY"))) {
            return null;
        }
        
        // Extract API key ID from authentication details (preferred method)
        if (authentication.getDetails() instanceof ApiKeyAuthenticationDetails) {
            ApiKeyAuthenticationDetails details = (ApiKeyAuthenticationDetails) authentication.getDetails();
            return details.getApiKeyId();
        }
        
        // Fallback: Try to get from ApiKey entity
        return getCurrentApiClient()
                .map(ApiKey::getId)
                .orElse(null);
    }
    
    /**
     * Get scopes for current API client.
     * Returns empty list if not authenticated via API key or no scopes configured.
     * 
     * @return List of scopes
     */
    public List<String> getCurrentApiClientScopes() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return List.of();
        }
        
        // Check if authenticated as API client
        if (!authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_API_KEY"))) {
            return List.of();
        }
        
        // Extract scopes from authentication details
        if (authentication.getDetails() instanceof ApiKeyAuthenticationDetails) {
            ApiKeyAuthenticationDetails details = (ApiKeyAuthenticationDetails) authentication.getDetails();
            return details.getScopes();
        }
        
        return List.of();
    }
    
    /**
     * Check if current request is authenticated as API client.
     * 
     * @return true if authenticated as API client
     */
    public boolean isApiClient() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_API_KEY"));
    }
    
    /**
     * Get API key principal (hashed key string).
     * 
     * @return API key principal or null
     */
    public String getApiKeyPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        boolean isApiKeyAuth = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_API_KEY"));
        
        if (!isApiKeyAuth) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        return principal != null ? principal.toString() : null;
    }
}

