package com.tiktel.ttelgo.integration.stripe;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class StripeClient {
    
    private final StripeConfig stripeConfig;
    
    /**
     * Create a payment intent for an order
     * @param amount Amount in the smallest currency unit (e.g., cents for USD)
     * @param currency Currency code (e.g., "usd")
     * @param orderId Order ID for metadata
     * @param customerEmail Customer email
     * @return PaymentIntent object
     */
    public PaymentIntent createPaymentIntent(
            BigDecimal amount,
            String currency,
            Long orderId,
            String customerEmail) throws StripeException {
        
        // Convert amount to smallest currency unit (cents for USD)
        long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
        
        // Build payment intent params
        PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(currency.toLowerCase())
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                );
        
        // Add metadata - Stripe Java SDK 24.x uses putMetadata method
        // Note: Metadata is optional, payment will work without it
        if (orderId != null) {
            paramsBuilder.putMetadata("order_id", String.valueOf(orderId));
        }
        if (customerEmail != null && !customerEmail.trim().isEmpty()) {
            paramsBuilder.putMetadata("customer_email", customerEmail);
        }
        
        PaymentIntentCreateParams params = paramsBuilder.build();
        
        PaymentIntent paymentIntent = PaymentIntent.create(params);
        log.info("Created payment intent: {} for order: {}", paymentIntent.getId(), orderId);
        
        return paymentIntent;
    }
    
    /**
     * Retrieve a payment intent by ID
     */
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }
    
    /**
     * Confirm a payment intent (usually done client-side, but can be done server-side)
     */
    public PaymentIntent confirmPaymentIntent(String paymentIntentId) throws StripeException {
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        return paymentIntent.confirm();
    }
}
