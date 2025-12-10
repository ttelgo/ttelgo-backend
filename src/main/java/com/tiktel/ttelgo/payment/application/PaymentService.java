package com.tiktel.ttelgo.payment.application;

import com.tiktel.ttelgo.integration.stripe.StripeClient;
import com.tiktel.ttelgo.integration.stripe.StripeConfig;
import com.tiktel.ttelgo.order.application.port.OrderRepositoryPort;
import com.tiktel.ttelgo.order.domain.Order;
import com.tiktel.ttelgo.order.domain.OrderStatus;
import com.tiktel.ttelgo.order.domain.PaymentStatus;
import com.tiktel.ttelgo.payment.api.dto.CreatePaymentIntentRequest;
import com.tiktel.ttelgo.payment.api.dto.CreatePaymentIntentResponse;
import com.tiktel.ttelgo.payment.application.port.PaymentRepositoryPort;
import com.tiktel.ttelgo.payment.domain.Payment;
import com.tiktel.ttelgo.plan.application.PlanService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final StripeClient stripeClient;
    private final StripeConfig stripeConfig;
    private final PaymentRepositoryPort paymentRepository;
    private final OrderRepositoryPort orderRepository;
    private final PlanService planService;
    
    /**
     * Create a payment intent for a new order
     * This creates both the order (pending) and payment intent
     */
    @Transactional
    public CreatePaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request) throws StripeException {
        // Step 1: Get bundle price from eSIMGo if amount not provided
        BigDecimal amount = request.getAmount();
        if (amount == null && request.getBundleId() != null) {
            try {
                var bundleDetails = planService.getBundleDetails(request.getBundleId());
                if (bundleDetails != null && bundleDetails.getPrice() != null) {
                    amount = BigDecimal.valueOf(bundleDetails.getPrice());
                    log.info("Retrieved bundle price from eSIMGo: {} for bundle: {}", amount, request.getBundleId());
                } else {
                    throw new RuntimeException("Bundle price not found for bundle: " + request.getBundleId());
                }
            } catch (Exception e) {
                log.error("Error fetching bundle price: {}", e.getMessage());
                throw new RuntimeException("Failed to get bundle price: " + e.getMessage());
            }
        }
        
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        
        // Step 2: Create order in PENDING status
        Order order = Order.builder()
                .orderReference(UUID.randomUUID().toString())
                .bundleId(request.getBundleId())
                .bundleName(request.getBundleName())
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .totalAmount(amount)
                .currency(request.getCurrency() != null ? request.getCurrency().toUpperCase() : "USD")
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        
        Order savedOrder = orderRepository.save(order);
        log.info("Created pending order: {} for bundle: {}", savedOrder.getId(), request.getBundleId());
        
        // Step 3: Create Stripe payment intent
        PaymentIntent paymentIntent = stripeClient.createPaymentIntent(
                amount,
                request.getCurrency() != null ? request.getCurrency() : "usd",
                savedOrder.getId(),
                request.getCustomerEmail()
        );
        
        // Step 4: Save payment record
        Payment payment = Payment.builder()
                .orderId(savedOrder.getId())
                .paymentIntentId(paymentIntent.getId())
                .amount(amount)
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .status(PaymentStatus.PENDING)
                .paymentMethod("card") // Default, will be updated when payment is confirmed
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        paymentRepository.save(payment);
        log.info("Created payment intent: {} for order: {}", paymentIntent.getId(), savedOrder.getId());
        
        // Step 5: Return response with client secret
        return CreatePaymentIntentResponse.builder()
                .clientSecret(paymentIntent.getClientSecret())
                .paymentIntentId(paymentIntent.getId())
                .publishableKey(stripeConfig.getPublishableKey())
                .orderId(savedOrder.getId())
                .build();
    }
    
    /**
     * Confirm payment and update order status
     * Called after Stripe payment is confirmed (via webhook or client confirmation)
     */
    @Transactional
    public void confirmPayment(String paymentIntentId) throws StripeException {
        // Retrieve payment intent from Stripe
        PaymentIntent paymentIntent = stripeClient.retrievePaymentIntent(paymentIntentId);
        
        // Find payment record
        paymentRepository.findByPaymentIntentId(paymentIntentId)
                .ifPresentOrElse(
                    payment -> {
                        // Update payment status
                        if ("succeeded".equals(paymentIntent.getStatus())) {
                            payment.setStatus(PaymentStatus.SUCCESS);
                            payment.setChargeId(paymentIntent.getLatestCharge());
                            payment.setUpdatedAt(LocalDateTime.now());
                            paymentRepository.save(payment);
                            
                            // Update order payment status
                            orderRepository.findById(payment.getOrderId())
                                    .ifPresent(order -> {
                                        order.setPaymentStatus(PaymentStatus.SUCCESS);
                                        // Order status will be updated to COMPLETED after eSIM activation
                                        orderRepository.save(order);
                                        log.info("Payment confirmed for order: {}", order.getId());
                                    });
                        } else if ("canceled".equals(paymentIntent.getStatus()) || 
                                   "payment_failed".equals(paymentIntent.getStatus())) {
                            payment.setStatus(PaymentStatus.FAILED);
                            payment.setFailureReason(paymentIntent.getLastPaymentError() != null 
                                    ? paymentIntent.getLastPaymentError().getMessage() 
                                    : "Payment failed");
                            payment.setUpdatedAt(LocalDateTime.now());
                            paymentRepository.save(payment);
                            
                            // Update order payment status
                            orderRepository.findById(payment.getOrderId())
                                    .ifPresent(order -> {
                                        order.setPaymentStatus(PaymentStatus.FAILED);
                                        orderRepository.save(order);
                                        log.warn("Payment failed for order: {}", order.getId());
                                    });
                        }
                    },
                    () -> log.warn("Payment record not found for payment intent: {}", paymentIntentId)
                );
    }
}
