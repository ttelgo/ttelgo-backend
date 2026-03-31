package com.tiktel.ttelgo.webhook.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.exception.BusinessException;
import com.tiktel.ttelgo.webhook.application.StripeWebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
            @RequestHeader(value = "Stripe-Signature", required = false) String signatureHeader) {
        
        log.info("Received Stripe webhook");

        if (!StringUtils.hasText(payload)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid webhook payload"));
        }

        if (!StringUtils.hasText(signatureHeader)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Missing Stripe-Signature header"));
        }

        try {
            stripeWebhookService.processWebhook(payload, signatureHeader);
            return ResponseEntity.ok(ApiResponse.success("Webhook processed successfully"));
        } catch (BusinessException e) {
            log.warn("Stripe webhook rejected: {}", e.getMessage());
            return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid webhook request"));
        }
    }
}

