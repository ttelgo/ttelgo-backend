package com.tiktel.ttelgo.apikey.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_usage_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiUsageLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "api_key_id", nullable = false)
    private Long apiKeyId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_key_id", insertable = false, updatable = false)
    private ApiKey apiKey;
    
    @Column(name = "endpoint", nullable = false, length = 500)
    private String endpoint;
    
    @Column(name = "method", nullable = false, length = 10)
    private String method;
    
    @Column(name = "status_code")
    private Integer statusCode;
    
    @Column(name = "response_time_ms")
    private Integer responseTimeMs;
    
    @Column(name = "request_size_bytes")
    private Integer requestSizeBytes;
    
    @Column(name = "response_size_bytes")
    private Integer responseSizeBytes;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;
    
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

