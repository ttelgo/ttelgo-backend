package com.tiktel.ttelgo.apikey.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * API Scope Validator - validates API client scopes for endpoint access.
 * Enforces scope-based authorization for API client requests.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiScopeValidator {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ApiClientResolver apiClientResolver;
    
    /**
     * Validate if the given scope is allowed for the endpoint.
     * 
     * @param scopesJson JSON array of scopes (e.g., ["GET:/api/v1/orders", "POST:/api/v1/orders"])
     * @param endpoint Request endpoint path (e.g., "/api/v1/orders")
     * @param method HTTP method (e.g., "GET", "POST")
     * @return true if scope allows access to endpoint
     */
    public boolean isScopeAllowed(String scopesJson, String endpoint, String method) {
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
                    // Just path pattern (method-agnostic)
                    if (pathMatcher.match(scope, endpoint) || scope.equals("*")) {
                        return true;
                    }
                }
            }
            
            return false;
            
        } catch (Exception e) {
            log.warn("Error validating scope: {}", e.getMessage());
            return false; // Fail secure - deny access if scope check fails
        }
    }
    
    /**
     * Validate scope for current request.
     * Extracts endpoint and method from request and validates against stored scopes.
     * 
     * @param storedScopes Scopes stored in API key (JSON array string)
     * @param requestEndpoint Request endpoint path
     * @param requestMethod HTTP method
     * @return true if scope allows access
     */
    public boolean validateScope(String storedScopes, String requestEndpoint, String requestMethod) {
        return isScopeAllowed(storedScopes, requestEndpoint, requestMethod);
    }
    
    /**
     * Validate scope from current request's authentication context.
     * 
     * @param endpoint Endpoint path
     * @param method HTTP method
     * @return true if scope allows access
     */
    public boolean validateCurrentScope(String endpoint, String method) {
        List<String> scopes = apiClientResolver.getCurrentApiClientScopes();
        return validateScope(scopes, endpoint, method);
    }
    
    /**
     * Validate scope using List of scopes (for programmatic use).
     * 
     * @param scopes List of scopes (e.g., ["GET:/api/v1/orders", "/api/v1/orders/**"])
     * @param endpoint Endpoint path (e.g., "/api/v1/orders/123")
     * @param method HTTP method (e.g., "GET", "POST")
     * @return true if scope allows access, false otherwise
     */
    public boolean validateScope(List<String> scopes, String endpoint, String method) {
        if (scopes == null || scopes.isEmpty()) {
            return true; // No scopes means all allowed
        }
        
        // Check for wildcard
        if (scopes.contains("*")) {
            return true;
        }
        
        // Check exact match: METHOD:/api/endpoint
        String methodAndPath = method + ":" + endpoint;
        if (scopes.contains(methodAndPath)) {
            return true;
        }
        
        // Check pattern matching (e.g., GET:/api/v1/orders/** matches GET:/api/v1/orders/123)
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
                // Just path pattern (all methods allowed)
                if (pathMatcher.match(scope, endpoint) || scope.equals("*")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if current authentication has required scope.
     * This is a convenience method for checking scopes in controllers/services.
     * 
     * @param requiredScope Required scope (e.g., "orders:read", "GET:/api/v1/orders")
     * @return true if current API client has the required scope
     */
    public boolean hasScope(String requiredScope) {
        List<String> scopes = apiClientResolver.getCurrentApiClientScopes();
        return scopes.contains(requiredScope) || scopes.contains("*");
    }
}

