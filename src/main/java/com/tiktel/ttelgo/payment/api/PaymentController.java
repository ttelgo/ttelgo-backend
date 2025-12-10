package com.tiktel.ttelgo.payment.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.payment.api.dto.CreatePaymentIntentRequest;
import com.tiktel.ttelgo.payment.api.dto.CreatePaymentIntentResponse;
import com.tiktel.ttelgo.payment.application.PaymentService;
import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    
    private final PaymentService paymentService;
    
    /**
     * Create a payment intent for checkout
     * POST /api/payments/intent
     */
    @PostMapping("/intent")
    public ResponseEntity<ApiResponse<CreatePaymentIntentResponse>> createPaymentIntent(
            @Valid @RequestBody CreatePaymentIntentRequest request) {
        try {
            CreatePaymentIntentResponse response = paymentService.createPaymentIntent(request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (StripeException e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to create payment intent: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Confirm payment (usually called after client-side confirmation)
     * POST /api/payments/confirm
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPayment(
            @RequestParam String paymentIntentId) {
        try {
            paymentService.confirmPayment(paymentIntentId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (StripeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to confirm payment: " + e.getMessage()));
        }
    }
}
