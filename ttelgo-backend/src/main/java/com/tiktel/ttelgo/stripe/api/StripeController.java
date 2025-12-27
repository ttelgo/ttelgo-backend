package com.tiktel.ttelgo.stripe.api;

import com.stripe.exception.StripeException;
import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.stripe.api.dto.CreateCheckoutSessionRequest;
import com.tiktel.ttelgo.stripe.api.dto.CreateCheckoutSessionResponse;
import com.tiktel.ttelgo.stripe.api.dto.SessionDetailsResponse;
import com.tiktel.ttelgo.stripe.application.StripeCheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stripe")
@RequiredArgsConstructor
@Slf4j
public class StripeController {

    private final StripeCheckoutService stripeCheckoutService;

    @PostMapping("/checkout-session")
    public ResponseEntity<ApiResponse<CreateCheckoutSessionResponse>> createCheckoutSession(
            @Valid @RequestBody CreateCheckoutSessionRequest request) throws StripeException {
        CreateCheckoutSessionResponse response = stripeCheckoutService.createCheckoutSession(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<SessionDetailsResponse>> getSession(
            @PathVariable String sessionId) throws StripeException {
        SessionDetailsResponse response = stripeCheckoutService.getSessionDetails(sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping({"/webhook", "/webhooks/stripe"})
    public ResponseEntity<String> handleWebhook(
            @RequestHeader("Stripe-Signature") String signature,
            @RequestBody String payload) {
        stripeCheckoutService.handleWebhook(payload, signature);
        return ResponseEntity.ok("received");
    }
}

