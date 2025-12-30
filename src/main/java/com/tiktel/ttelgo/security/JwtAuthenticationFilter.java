package com.tiktel.ttelgo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
<<<<<<< HEAD
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
=======
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
>>>>>>> 517cfdbabcad5678433bdd3ff85dacd99c0cfaeb
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
<<<<<<< HEAD
import java.util.Collections;

=======

/**
 * JWT Authentication Filter that intercepts HTTP requests and validates JWT tokens.
 * 
 * This filter:
 * 1. Extracts JWT token from Authorization header (Bearer token)
 * 2. Validates the token
 * 3. Loads user details and sets authentication in Spring Security context
 * 4. Allows the request to proceed if authentication is successful
 */
>>>>>>> 517cfdbabcad5678433bdd3ff85dacd99c0cfaeb
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
<<<<<<< HEAD
    private final JwtTokenProvider jwtTokenProvider;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
=======
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String HEADER_NAME = "Authorization";
    
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
>>>>>>> 517cfdbabcad5678433bdd3ff85dacd99c0cfaeb
        
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
<<<<<<< HEAD
                Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
                String email = jwtTokenProvider.getEmailFromToken(jwt);
                
                // Create authentication token
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        email, 
                        null, 
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Set user ID as a custom detail for easy access in controllers
                request.setAttribute("userId", userId);
                request.setAttribute("userEmail", email);
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("JWT authentication successful for user: {} (ID: {})", email, userId);
            }
        } catch (Exception e) {
            log.error("Could not set user authentication in security context", e);
=======
                // Verify this is an access token, not a refresh token
                String tokenType = jwtTokenProvider.getTokenTypeFromToken(jwt);
                if (!"refresh".equals(tokenType)) {
                    Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
                    
                    // Load user details
                    UserDetails userDetails = userDetailsService.loadUserById(userId);
                    
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
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
>>>>>>> 517cfdbabcad5678433bdd3ff85dacd99c0cfaeb
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
<<<<<<< HEAD
     * Extract JWT token from Authorization header
     * Expected format: "Bearer <token>"
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
=======
     * Extract JWT token from Authorization header.
     * Expected format: "Bearer <token>"
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(HEADER_NAME);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        
>>>>>>> 517cfdbabcad5678433bdd3ff85dacd99c0cfaeb
        return null;
    }
}

