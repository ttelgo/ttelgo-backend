package com.tiktel.ttelgo.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class to resolve roles and scopes from Spring Security context.
 * Provides convenient methods to extract user information from authentication.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RoleScopeResolver {
    
    /**
     * Get current authenticated user ID from SecurityContext.
     * 
     * @return User ID or null if not authenticated
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        // Try to get user ID from UserPrincipal
        if (authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return userPrincipal.getId();
        }
        
        // JWT filter should have set the UserPrincipal
        // If not, user is not authenticated via JWT
        
        return null;
    }
    
    /**
     * Get current authenticated user email from SecurityContext.
     * 
     * @return User email or null if not authenticated
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        if (authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return userPrincipal.getUsername(); // email is stored as username
        }
        
        return authentication.getName();
    }
    
    /**
     * Get roles from current authentication.
     * 
     * @return List of role names (e.g., ["ROLE_USER", "ROLE_ADMIN"])
     */
    public List<String> getRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Collections.emptyList();
        }
        
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toList());
    }
    
    /**
     * Check if current user has a specific role.
     * 
     * @param role Role to check (e.g., "ADMIN", "USER")
     * @return true if user has the role
     */
    public boolean hasRole(String role) {
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return getRoles().contains(roleWithPrefix);
    }
    
    /**
     * Get scopes from JWT token (if available).
     * 
     * @return List of scopes or empty list
     */
    public List<String> getScopesFromRequest() {
        // This would require JWT token extraction
        // For now, return empty list
        // Can be enhanced to extract from JWT claims
        return Collections.emptyList();
    }
}

