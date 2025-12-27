package com.tiktel.ttelgo.stripe.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "stripe_webhook_events")
@Getter
@Setter
public class StripeWebhookEvent {

    @Id
    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @PrePersist
    public void onCreate() {
        if (receivedAt == null) {
            receivedAt = OffsetDateTime.now();
        }
    }
}

