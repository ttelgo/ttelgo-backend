package com.tiktel.ttelgo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiktel.ttelgo.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final com.tiktel.ttelgo.common.idempotency.infrastructure.filter.IdempotencyFilter idempotencyFilter;
    private final SecurityHeadersFilter securityHeadersFilter;
    
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                         ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                         com.tiktel.ttelgo.common.idempotency.infrastructure.filter.IdempotencyFilter idempotencyFilter,
                         SecurityHeadersFilter securityHeadersFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
        this.idempotencyFilter = idempotencyFilter;
        this.securityHeadersFilter = securityHeadersFilter;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",
            "http://localhost:3000",
            "http://localhost:8080",
            "https://www.ttelgo.com",
            "https://ttelgo.com",
            "http://www.ttelgo.com",
            "http://ttelgo.com"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);  // Match all /api paths
        source.registerCorsConfiguration("/api/v1/**", configuration);
        return source;
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Allow OPTIONS requests for CORS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Public static resources
                .requestMatchers("/uploads/**").permitAll()
                // Public auth endpoints
                .requestMatchers(
                    "/api/v1/auth/customer/**",
                    "/api/v1/auth/admin/login",
                    "/api/v1/auth/admin/create-initial",
                    "/api/v1/auth/google",
                    "/api/v1/auth/apple",
                    "/api/v1/auth/otp/**",
                    "/api/v1/auth/email/**",
                    "/api/v1/auth/register",
                    "/api/v1/auth/refresh"
                ).permitAll()
                // Public webhooks
                .requestMatchers("/api/v1/webhooks/**").permitAll()
                // Public read-only content
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/bundles/**",
                    "/api/v1/catalogue/**",
                    "/api/v1/posts/**",
                    "/api/v1/faqs/**",
                    "/api/v1/health/**"
                ).permitAll()
                // Protected user-specific endpoints — require valid JWT
                // /api/v1/users/** covers /users/me, /users/{id}/orders, etc.
                .requestMatchers("/api/v1/users/**").authenticated()
                // Protected order endpoints — require valid JWT
                .requestMatchers("/api/v1/orders/**").authenticated()
                // Protected eSIM endpoints — require valid JWT
                .requestMatchers("/api/v1/esims/**").authenticated()
                // Admin endpoints — require admin or super-admin role
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                // Everything else — permit (explicit per-controller auth via @PreAuthorize)
                .anyRequest().permitAll()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(customAuthenticationEntryPoint(objectMapper))
                .accessDeniedHandler(customAccessDeniedHandler(objectMapper))
            )
            .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(idempotencyFilter, ApiKeyAuthenticationFilter.class);
        
        return http.build();
    }
    
    private AuthenticationEntryPoint customAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            
            ApiResponse<Object> errorResponse = ApiResponse.error(
                "Authentication required. Please provide a valid authentication token."
            );
            
            objectMapper.writeValue(response.getWriter(), errorResponse);
        };
    }
    
    private AccessDeniedHandler customAccessDeniedHandler(ObjectMapper objectMapper) {
        return (HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            
            ApiResponse<Object> errorResponse = ApiResponse.error(
                "Access denied. You do not have permission to access this resource."
            );
            
            objectMapper.writeValue(response.getWriter(), errorResponse);
        };
    }
}
