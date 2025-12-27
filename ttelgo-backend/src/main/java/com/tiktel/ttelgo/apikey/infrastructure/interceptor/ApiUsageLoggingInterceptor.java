package com.tiktel.ttelgo.apikey.infrastructure.interceptor;

import com.tiktel.ttelgo.apikey.domain.ApiUsageLog;
import com.tiktel.ttelgo.apikey.infrastructure.repository.ApiUsageLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ApiUsageLoggingInterceptor implements HandlerInterceptor {
    
    private final ApiUsageLogRepository usageLogRepository;
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) throws Exception {
        
        Long apiKeyId = (Long) request.getAttribute("apiKeyId");
        if (apiKeyId == null) {
            return; // Not an API key request
        }
        
        Long startTime = (Long) request.getAttribute("requestStartTime");
        long responseTime = startTime != null ? System.currentTimeMillis() - startTime : 0;
        
        try {
            ApiUsageLog log = ApiUsageLog.builder()
                .apiKeyId(apiKeyId)
                .endpoint(request.getRequestURI())
                .method(request.getMethod())
                .statusCode(response.getStatus())
                .responseTimeMs((int) responseTime)
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .errorMessage(ex != null ? ex.getMessage() : null)
                .createdAt(LocalDateTime.now())
                .build();
            
            usageLogRepository.save(log);
        } catch (Exception e) {
            // Log error but don't fail the request
            System.err.println("Failed to log API usage: " + e.getMessage());
        }
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
                           Object handler) throws Exception {
        request.setAttribute("requestStartTime", System.currentTimeMillis());
        return true;
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}

