package com.tiktel.ttelgo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security headers filter to add OWASP-recommended HTTP security headers.
 * 
 * Headers added:
 * - X-Content-Type-Options: nosniff - Prevents MIME type sniffing
 * - X-Frame-Options: DENY - Prevents clickjacking attacks
 * - X-XSS-Protection: 1; mode=block - Enables XSS protection (legacy, but still useful)
 * - Strict-Transport-Security: max-age=31536000; includeSubDomains - Enforces HTTPS (HSTS)
 * - Content-Security-Policy: default-src 'self' - Restricts resource loading (basic policy)
 * - Referrer-Policy: strict-origin-when-cross-origin - Controls referrer information
 * - Permissions-Policy: Controls browser features (camera, microphone, etc.)
 */
@Slf4j
@Component
@Order(1) // Run early in the filter chain
public class SecurityHeadersFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // Prevent clickjacking attacks
        response.setHeader("X-Frame-Options", "DENY");
        
        // Enable XSS protection (legacy browsers)
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Enforce HTTPS (HSTS) - only for HTTPS connections
        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        }
        
        // Content Security Policy - restrict resource loading
        // For APIs, we allow 'self' and don't need inline scripts/styles
        response.setHeader("Content-Security-Policy", 
                "default-src 'self'; " +
                "script-src 'self'; " +
                "style-src 'self' 'unsafe-inline'; " + // Allow inline styles for Swagger UI
                "img-src 'self' data: https:; " +
                "font-src 'self' data:; " +
                "connect-src 'self'; " +
                "frame-ancestors 'none';"); // Equivalent to X-Frame-Options: DENY
        
        // Control referrer information
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions Policy (formerly Feature-Policy)
        response.setHeader("Permissions-Policy", 
                "geolocation=(), " +
                "microphone=(), " +
                "camera=(), " +
                "payment=(), " +
                "usb=(), " +
                "magnetometer=(), " +
                "gyroscope=(), " +
                "speaker=()");
        
        // Remove server header to hide technology stack (handled by server config, but good practice)
        // Note: Server header is typically set by the servlet container, not the application
        
        filterChain.doFilter(request, response);
    }
}

