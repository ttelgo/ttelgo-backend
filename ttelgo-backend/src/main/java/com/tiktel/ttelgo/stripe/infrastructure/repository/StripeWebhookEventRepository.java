package com.tiktel.ttelgo.stripe.infrastructure.repository;

import com.tiktel.ttelgo.stripe.domain.StripeWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, String> {
}

