package com.tiktel.ttelgo.payment.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.exception.BusinessException;
import com.tiktel.ttelgo.payment.infrastructure.adapter.StripeService;
import com.tiktel.ttelgo.order.application.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
    private final OrderService orderService;
    
    public PaymentController(StripeService stripeService, OrderService orderService) {
        this.stripeService = stripeService;
        this.orderService = orderService;
    }
    
    /**
     * Create payment intent (creates order + payment intent in one call)
     * This is the main endpoint used by the frontend
     */
    @Operation(summary = "Create payment intent", 
               description = "Create an order and Stripe PaymentIntent for checkout")
    @PostMapping("/intent")
    public ApiResponse<StripeService.PaymentIntentResponse> createPaymentIntent(
            @Valid @RequestBody CreatePaymentIntentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = extractUserId(authentication);
            String ipAddress = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            
            log.info("Creating payment intent: bundleId={}, amount={}, currency={}, userId={}", 
                    request.getBundleId(), request.getAmount(), request.getCurrency(), userId);
            
            // Ensure customer email is provided
            String customerEmail = request.getCustomerEmail();
            if (customerEmail == null || customerEmail.trim().isEmpty()) {
                customerEmail = "customer@example.com";
                log.warn("No customer email provided, using default: {}", customerEmail);
            }
            
            // Step 1: Create order first
            com.tiktel.ttelgo.order.domain.Order order;
            try {
                order = orderService.createB2COrder(
                        userId,
                        customerEmail,
                        request.getBundleId(),
                        request.getQuantity() != null ? request.getQuantity() : 1,
                        ipAddress,
                        userAgent != null ? userAgent : "Unknown"
                );
                log.info("Order created successfully: orderId={}", order.getId());
            } catch (Exception e) {
                log.error("Failed to create order: bundleId={}, error={}", request.getBundleId(), e.getMessage(), e);
                throw new RuntimeException("Failed to create order: " + e.getMessage(), e);
            }
            
            // Step 2: Create payment intent for the order
            StripeService.PaymentIntentResponse response;
            try {
                response = stripeService.createPaymentIntentForOrder(
                        order.getId(),
                        userId,
                        request.getAmount() != null ? BigDecimal.valueOf(request.getAmount()) : order.getTotalAmount(),
                        request.getCurrency() != null ? request.getCurrency() : order.getCurrency(),
                        customerEmail,
                        idempotencyKey
                );
                log.info("Payment intent created successfully: paymentIntentId={}, orderId={}", 
                        response.paymentIntentId(), response.orderId());
            } catch (BusinessException e) {
                // Re-throw BusinessException as-is (contains Stripe error details)
                log.error("Failed to create payment intent: orderId={}, error={}", order.getId(), e.getMessage(), e);
                throw e;
            } catch (Exception e) {
                log.error("Failed to create payment intent: orderId={}, error={}", order.getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to create payment intent: " + e.getMessage(), e);
            }
            
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("Error creating payment intent: {}", e.getMessage(), e);
            throw e; // Let global exception handler handle it
        }
    }
    
    /**
     * Confirm payment (called after client-side Stripe confirmation)
     */
    @Operation(summary = "Confirm payment", 
               description = "Confirm a payment after client-side Stripe confirmation")
    @PostMapping("/confirm")
    public ApiResponse<Void> confirmPayment(
            @RequestParam("paymentIntentId") String paymentIntentId,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        log.info("Confirming payment: paymentIntentId={}, userId={}", paymentIntentId, userId);
        
        // Payment is already confirmed on Stripe side via confirmCardPayment
        // This endpoint just acknowledges the confirmation
        // The webhook will handle the actual payment status update
        return ApiResponse.success(null);
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
        if (authentication == null) {
            return null; // Guest checkout
        }
        return 1L; // Placeholder
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatePaymentIntentRequest {
        @NotBlank(message = "Bundle ID is required")
        private String bundleId;
        
        private Double amount; // Optional - will use bundle price if not provided
        
        private String currency; // Optional - will use bundle currency if not provided
        
        @Email(message = "Invalid email format")
        private String customerEmail; // Optional - will use default if not provided
        
        private Integer quantity; // Optional - defaults to 1
        
        private String bundleName; // Optional - for display purposes
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

