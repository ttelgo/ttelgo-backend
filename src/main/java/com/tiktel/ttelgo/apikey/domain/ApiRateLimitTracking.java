package com.tiktel.ttelgo.apikey.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_rate_limit_tracking", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"api_key_id", "window_type", "window_start"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiRateLimitTracking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "api_key_id", nullable = false)
    private Long apiKeyId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_key_id", insertable = false, updatable = false)
    private ApiKey apiKey;
    
    @Column(name = "window_type", nullable = false, length = 20)
    private String windowType; // 'minute', 'hour', 'day'
    
    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;
    
    @Column(name = "request_count")
    @Builder.Default
    private Integer requestCount = 0;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

