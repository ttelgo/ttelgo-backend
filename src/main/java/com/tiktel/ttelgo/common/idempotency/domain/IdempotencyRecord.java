package com.tiktel.ttelgo.common.idempotency.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Stores idempotency records to prevent duplicate processing of write requests.
 * 
 * Key design:
 * - Composite key: idempotencyKey + method + path + actorId (allows same key on different endpoints/users)
 * - Request hash: SHA-256 of request body to detect conflicting payloads (409 if mismatch)
 * - Response cache: stores status code + body to replay on retry
 * - TTL: expires after 24h (configurable) to avoid unbounded growth
 * - Status: PENDING (in-flight) -> COMPLETED | FAILED
 */
@Entity
@Table(name = "idempotency_records", indexes = {
    @Index(name = "idx_idempotency_lookup", columnList = "idempotency_key,http_method,request_path,actor_id", unique = true),
    @Index(name = "idx_idempotency_expires_at", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey; // Client-provided UUID
    
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod; // GET, POST, PUT, PATCH, DELETE
    
    @Column(name = "request_path", nullable = false, length = 512)
    private String requestPath; // e.g., /api/v1/esim-orders
    
    @Column(name = "actor_id")
    private Long actorId; // User ID or API key ID (null for anonymous)
    
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash; // SHA-256 of request body (to detect payload conflicts)
    
    @Column(name = "response_status", nullable = false)
    private Integer responseStatus; // HTTP status code (200, 201, 400, etc.)
    
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody; // JSON response body to replay
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private IdempotencyStatus status = IdempotencyStatus.PENDING;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt; // TTL: 24h from creation
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (expiresAt == null) {
            expiresAt = now.plusHours(24); // Default TTL: 24 hours
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum IdempotencyStatus {
        PENDING,    // Request is in-flight
        COMPLETED,  // Request completed successfully
        FAILED      // Request failed (non-2xx status)
    }
}

