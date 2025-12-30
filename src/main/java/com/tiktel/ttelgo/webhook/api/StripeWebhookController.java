package com.tiktel.ttelgo.webhook.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.webhook.application.StripeWebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Stripe webhook endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks/stripe")
public class StripeWebhookController {
    
    private final StripeWebhookService stripeWebhookService;
    
    public StripeWebhookController(StripeWebhookService stripeWebhookService) {
        this.stripeWebhookService = stripeWebhookService;
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<String>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader) {
        
        log.info("Received Stripe webhook");
        
        try {
            stripeWebhookService.processWebhook(payload, signatureHeader);
            return ResponseEntity.ok(ApiResponse.success("Webhook processed successfully"));
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            // Return 200 to acknowledge receipt even if processing fails
            // Stripe will retry if we return an error
            return ResponseEntity.ok(ApiResponse.success("Webhook received"));
        }
    }
}

