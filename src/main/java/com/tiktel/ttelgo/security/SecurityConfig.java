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
                .requestMatchers(
                    // Public endpoints that don't require authentication:
                    "/api/v1/auth/**",
                    "/api/v1/health/**",
                    "/api/v1/bundles/**",
                    "/api/v1/faqs/**",
                    "/api/v1/posts/**",
                    "/api/v1/webhooks/stripe/**",
                    "/api-docs/**",  // API documentation
                    "/v3/api-docs/**",  // OpenAPI docs
                    "/swagger-ui/**",  // Swagger UI
                    "/swagger-ui.html",
                    "/swagger-ui/index.html",
                    "/actuator/**",  // Spring Boot Actuator
                    "/error"  // Error pages
                ).permitAll()
                // Admin endpoints require authentication (JWT with ADMIN/SUPER_ADMIN role or API key)
                // Role-based access is enforced via @PreAuthorize annotations on controllers
                .requestMatchers("/api/v1/admin/**").authenticated()
                // All other endpoints require authentication (JWT or API key)
                .anyRequest().authenticated()
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

