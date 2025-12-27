package com.tiktel.ttelgo.payment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentIntentResponse {
    private String clientSecret; // For Stripe Elements on frontend
    private String paymentIntentId;
    private String publishableKey; // Stripe publishable key for frontend
    private Long orderId; // Created order ID (if order was created)
}

