package com.tiktel.ttelgo.webhook.infrastructure.repository;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "webhook_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEventJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String source; // ESIMGO, STRIPE
    
    @Column(name = "event_id", nullable = false)
    private String eventId; // External event ID
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    @Type(JsonBinaryType.class)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;
    
    @Column(nullable = false)
    private Boolean processed = false;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "processing_attempts")
    private Integer processingAttempts = 0;
    
    @Column(name = "last_processing_attempt_at")
    private LocalDateTime lastProcessingAttemptAt;
    
    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;
    
    @Column(name = "order_id")
    private Long orderId;
    
    @Column(name = "payment_id")
    private Long paymentId;
    
    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;
    
    @PrePersist
    protected void onCreate() {
        receivedAt = LocalDateTime.now();
    }
}

