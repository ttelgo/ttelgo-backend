package com.tiktel.ttelgo.config;

import com.tiktel.ttelgo.apikey.infrastructure.interceptor.ApiUsageLoggingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    private final ApiUsageLoggingInterceptor apiUsageLoggingInterceptor;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    
    public WebConfig(ApiUsageLoggingInterceptor apiUsageLoggingInterceptor) {
        this.apiUsageLoggingInterceptor = apiUsageLoggingInterceptor;
    }

    /**
     * Serve uploaded files (profile pictures, etc.) at /uploads/**
     * Maps to the local filesystem upload directory.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absoluteUploadPath = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absoluteUploadPath);
    }
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/uploads/**")
                .allowedOrigins(
                    "http://localhost:5173",
                    "http://localhost:3000",
                    "https://www.ttelgo.com",
                    "https://ttelgo.com"
                )
                .allowedMethods("GET")
                .maxAge(3600);
        registry.addMapping("/api/**")
                .allowedOrigins(
                    "http://localhost:5173", 
                    "http://localhost:3000", 
                    "http://localhost:8080",
                    "https://www.ttelgo.com",
                    "https://ttelgo.com",
                    "http://www.ttelgo.com",
                    "http://ttelgo.com"
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiUsageLoggingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/health/**", "/api-docs/**", "/swagger-ui/**");
    }
}

