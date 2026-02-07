package com.tiktel.ttelgo.payment.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.integration.stripe.StripeConfig;
import com.tiktel.ttelgo.integration.stripe.StripeService;
import com.tiktel.ttelgo.order.application.OrderService;
import com.tiktel.ttelgo.order.domain.Order;
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
@Tag(name = "Payments", description = "Payment processing")
public class PaymentController {
    
    private final StripeService stripeService;
    private final OrderService orderService;
    private final StripeConfig stripeConfig;
    
    public PaymentController(StripeService stripeService,
                             OrderService orderService,
                             StripeConfig stripeConfig) {
        this.stripeService = stripeService;
        this.orderService = orderService;
        this.stripeConfig = stripeConfig;
    }
    
    /**
     * One-shot: create order + PaymentIntent for checkout (used by frontend).
     * POST /api/v1/payments/intent - no auth required for guest checkout.
     */
    @Operation(summary = "Create order and payment intent", 
               description = "Creates an eSIM order and Stripe PaymentIntent in one call. Returns clientSecret for Stripe Elements.")
    @PostMapping("/intent")
    public ApiResponse<CheckoutPaymentIntentResponse> createCheckoutPaymentIntent(
            @Valid @RequestBody CreateCheckoutPaymentIntentRequest request,
            HttpServletRequest httpRequest,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        String customerEmail = request.getCustomerEmail() != null && !request.getCustomerEmail().isBlank()
                ? request.getCustomerEmail()
                : "customer@example.com";
        String bundleCode = request.getBundleId();
        int quantity = request.getQuantity() != null && request.getQuantity() >= 1 ? request.getQuantity() : 1;
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO;
        String currency = request.getCurrency() != null && !request.getCurrency().isBlank()
                ? request.getCurrency()
                : "usd";
        
        log.info("Creating order + payment intent: bundleId={}, amount={}, userId={}", bundleCode, amount, userId);
        
        Order order = orderService.createB2COrder(
                userId,
                customerEmail,
                bundleCode,
                quantity,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent") != null ? httpRequest.getHeader("User-Agent") : ""
        );
        
        StripeService.PaymentIntentResponse stripeResponse = stripeService.createPaymentIntentForOrder(
                order.getId(),
                userId,
                amount.compareTo(BigDecimal.ZERO) > 0 ? amount : order.getTotalAmount(),
                currency,
                customerEmail,
                null
        );
        
        CheckoutPaymentIntentResponse response = new CheckoutPaymentIntentResponse(
                order.getId(),
                stripeResponse.clientSecret(),
                stripeResponse.paymentIntentId(),
                stripeConfig.getPublishableKey(),
                stripeResponse.amount(),
                stripeResponse.currency(),
                stripeResponse.status()
        );
        return ApiResponse.success(response);
    }
    
    @Operation(summary = "Create payment intent for existing order", 
               description = "Create a Stripe PaymentIntent for an existing order")
    @PostMapping("/intents/orders")
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<StripeService.PaymentIntentResponse> createOrderPaymentIntent(
            @Valid @RequestBody CreateOrderPaymentIntentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        log.info("Creating payment intent for order: orderId={}, userId={}", 
                request.getOrderId(), userId);
        
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
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() != null) {
            try {
                Object principal = authentication.getPrincipal();
                if (principal instanceof java.util.Map) {
                    Object id = ((java.util.Map<?, ?>) principal).get("id");
                    if (id instanceof Number) return ((Number) id).longValue();
                }
            } catch (Exception ignored) {}
        }
        return 1L; // Guest or placeholder
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCheckoutPaymentIntentRequest {
        @NotBlank(message = "Bundle ID is required")
        private String bundleId;
        private BigDecimal amount;
        private String currency;
        private String bundleName;
        private Integer quantity;
        private String customerEmail;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutPaymentIntentResponse {
        private Long orderId;
        private String clientSecret;
        private String paymentIntentId;
        private String publishableKey;
        private BigDecimal amount;
        private String currency;
        private String status;
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

