package com.tiktel.ttelgo.common.idempotency;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecordJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "vendor_id")
    private Long vendorId;
    
    @Column(nullable = false)
    private String endpoint;
    
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;
    
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;
    
    @Column(name = "response_status_code")
    private Integer responseStatusCode;
    
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;
    
    @Column(name = "created_resource_type", length = 100)
    private String createdResourceType;
    
    @Column(name = "created_resource_id")
    private Long createdResourceId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

