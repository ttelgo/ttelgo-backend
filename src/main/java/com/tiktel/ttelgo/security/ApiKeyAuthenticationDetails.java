package com.tiktel.ttelgo.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * Authentication details for API key-based authentication.
 * Stores API key information in the Spring Security authentication context.
 */
@Data
public class ApiKeyAuthenticationDetails {
    
    private final Long apiKeyId;
    private final Long vendorId;
    private final String apiKeyName;
    private final List<String> scopes;
    private final HttpServletRequest request;
    
    public ApiKeyAuthenticationDetails(Long apiKeyId, Long vendorId, String apiKeyName, List<String> scopes, HttpServletRequest request) {
        this.apiKeyId = apiKeyId;
        this.vendorId = vendorId;
        this.apiKeyName = apiKeyName;
        this.scopes = scopes != null ? scopes : Collections.emptyList();
        this.request = request;
    }
    
    public ApiKeyAuthenticationDetails(Long apiKeyId, HttpServletRequest request) {
        this(apiKeyId, null, null, Collections.emptyList(), request);
    }
    
    public List<String> getScopes() {
        return scopes != null ? scopes : Collections.emptyList();
    }
}

