package com.tiktel.ttelgo.payment.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.integration.stripe.StripeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Payment API for creating PaymentIntents
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Payments", description = "Payment processing")
public class PaymentController {
    
    private final StripeService stripeService;
    
    public PaymentController(StripeService stripeService) {
        this.stripeService = stripeService;
    }
    
    @Operation(summary = "Create payment intent for order", 
               description = "Create a Stripe PaymentIntent for order payment")
    @PostMapping("/intents/orders")
    public ApiResponse<StripeService.PaymentIntentResponse> createOrderPaymentIntent(
            @Valid @RequestBody CreateOrderPaymentIntentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        log.info("Creating payment intent for order: orderId={}, userId={}", 
                request.getOrderId(), userId);
        
        // TODO: Validate order belongs to user
        // TODO: Validate order hasn't been paid
        
        StripeService.PaymentIntentResponse response = stripeService.createPaymentIntentForOrder(
                request.getOrderId(),
                userId,
                request.getAmount(),
                request.getCurrency() != null ? request.getCurrency() : "USD",
                request.getCustomerEmail(),
                idempotencyKey
        );
        
        return ApiResponse.success(response);
    }
    
    private Long extractUserId(Authentication authentication) {
        // TODO: Extract user ID from JWT token
        return 1L; // Placeholder
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrderPaymentIntentRequest {
        @NotNull(message = "Order ID is required")
        private Long orderId;
        
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.50", message = "Amount must be at least 0.50")
        private BigDecimal amount;
        
        private String currency;
        
        @Email(message = "Invalid email format")
        private String customerEmail;
    }
}

