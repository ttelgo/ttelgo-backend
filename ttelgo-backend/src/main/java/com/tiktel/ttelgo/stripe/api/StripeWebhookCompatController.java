package com.tiktel.ttelgo.stripe.api;

import com.tiktel.ttelgo.stripe.application.StripeCheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Backward compatible webhook endpoint to support existing scripts that post to /api/v1/webhooks/stripe.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class StripeWebhookCompatController {

    private final StripeCheckoutService stripeCheckoutService;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader("Stripe-Signature") String signature,
            @RequestBody String payload) {
        stripeCheckoutService.handleWebhook(payload, signature);
        return ResponseEntity.ok("received");
    }
}

