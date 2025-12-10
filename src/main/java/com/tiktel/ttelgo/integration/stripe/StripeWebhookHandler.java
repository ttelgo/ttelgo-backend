package com.tiktel.ttelgo.integration.stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.tiktel.ttelgo.payment.application.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/stripe")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookHandler {
    
    private final PaymentService paymentService;
    
    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;
    
    /**
     * Handle Stripe webhook events
     * POST /api/webhooks/stripe
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        if (webhookSecret == null || webhookSecret.trim().isEmpty()) {
            log.warn("Stripe webhook secret not configured, skipping signature verification");
            // In development, you might want to process without verification
            // In production, always verify the signature
        } else {
            try {
                Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
                log.info("Received Stripe webhook event: {} (id: {})", event.getType(), event.getId());
                
                // Handle the event
                if ("payment_intent.succeeded".equals(event.getType())) {
                    PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                            .getObject().orElse(null);
                    if (paymentIntent != null) {
                        try {
                            paymentService.confirmPayment(paymentIntent.getId());
                            log.info("Payment confirmed via webhook: {}", paymentIntent.getId());
                        } catch (Exception e) {
                            log.error("Error confirming payment: {}", e.getMessage(), e);
                        }
                    }
                } else if ("payment_intent.payment_failed".equals(event.getType())) {
                    PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                            .getObject().orElse(null);
                    if (paymentIntent != null) {
                        try {
                            paymentService.confirmPayment(paymentIntent.getId());
                            log.info("Payment failure processed via webhook: {}", paymentIntent.getId());
                        } catch (Exception e) {
                            log.error("Error processing payment failure: {}", e.getMessage(), e);
                        }
                    }
                }
                
                return ResponseEntity.ok().body("{\"received\": true}");
            } catch (SignatureVerificationException e) {
                log.error("Invalid webhook signature: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("{\"error\": \"Invalid signature\"}");
            } catch (Exception e) {
                log.error("Error processing webhook: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"error\": \"Internal error\"}");
            }
        }
        
        return ResponseEntity.ok().body("{\"received\": true}");
    }
}
