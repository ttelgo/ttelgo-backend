package com.tiktel.ttelgo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter that intercepts HTTP requests and validates JWT tokens.
 * 
 * This filter:
 * 1. Extracts JWT token from Authorization header (Bearer token)
 * 2. Validates the token
 * 3. Loads user details and sets authentication in Spring Security context
 * 4. Allows the request to proceed if authentication is successful
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String HEADER_NAME = "Authorization";
    
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        // Skip authentication for OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt)) {
                log.info("JWT token found in request, validating...");
                boolean isValid = jwtTokenProvider.validateToken(jwt);
                log.info("JWT token validation result: {}", isValid);
                
                if (isValid) {
                // Verify this is an access token, not a refresh token
                String tokenType = jwtTokenProvider.getTokenTypeFromToken(jwt);
                if (!"refresh".equals(tokenType)) {
                    Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
                    String email = jwtTokenProvider.getEmailFromToken(jwt);
                    String role = jwtTokenProvider.getRoleFromToken(jwt);
                    
                    UserDetails userDetails;
                    try {
                        // Try to load user from database
                        userDetails = userDetailsService.loadUserById(userId);
                        log.debug("JWT authentication successful for user from database: {}", email);
                    } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
                        // User doesn't exist in database, create UserPrincipal from JWT claims
                        // This allows stateless JWT authentication for testing
                        log.debug("User not found in database, creating UserPrincipal from JWT claims for userId: {}, email: {}", userId, email);
                        userDetails = UserPrincipal.createFromJwt(userId, email, role);
                    }
                    
                    // Create authentication token
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                        );
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Set authentication in Spring Security context
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("JWT authentication successful for user: {}", userDetails.getUsername());
                } else {
                    log.warn("Refresh token used in Authorization header - rejecting");
                }
                } else {
                    log.error("JWT token validation failed for token: {}", jwt.substring(0, Math.min(20, jwt.length())) + "...");
                }
            } else {
                log.debug("No JWT token found in request");
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract JWT token from Authorization header.
     * Expected format: "Bearer <token>"
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(HEADER_NAME);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        
        return null;
    }
}
