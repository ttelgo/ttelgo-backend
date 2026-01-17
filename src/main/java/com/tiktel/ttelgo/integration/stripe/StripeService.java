package com.tiktel.ttelgo.integration.stripe;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.tiktel.ttelgo.common.domain.enums.PaymentStatus;
import com.tiktel.ttelgo.common.domain.enums.PaymentType;
import com.tiktel.ttelgo.common.exception.BusinessException;
import com.tiktel.ttelgo.common.exception.ErrorCode;
import com.tiktel.ttelgo.payment.domain.Payment;
import com.tiktel.ttelgo.payment.infrastructure.mapper.PaymentMapper;
import com.tiktel.ttelgo.payment.infrastructure.repository.PaymentJpaEntity;
import com.tiktel.ttelgo.payment.infrastructure.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stripe service for payment processing
 */
@Slf4j
@Service
public class StripeService {
    
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final StripeConfig stripeConfig;
    
    public StripeService(PaymentRepository paymentRepository,
                        PaymentMapper paymentMapper,
                        StripeConfig stripeConfig) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.stripeConfig = stripeConfig;
    }
    
    /**
     * Create PaymentIntent for B2C order
     */
    @Transactional
    public PaymentIntentResponse createPaymentIntentForOrder(Long orderId, Long userId,
                                                             BigDecimal amount, String currency,
                                                             String customerEmail,
                                                             String idempotencyKey) {
        log.info("Creating PaymentIntent for order: orderId={}, amount={}, currency={}",
                orderId, amount, currency);
        
        try {
            // Create payment record
            Payment payment = Payment.builder()
                    .paymentNumber(generatePaymentNumber())
                    .type(PaymentType.B2C_ORDER)
                    .orderId(orderId)
                    .userId(userId)
                    .amount(amount)
                    .refundedAmount(BigDecimal.ZERO)
                    .currency(currency.toUpperCase())
                    .status(PaymentStatus.CREATED)
                    .customerEmail(customerEmail)
                    .idempotencyKey(idempotencyKey)
                    .build();
            
            PaymentJpaEntity savedPayment = paymentRepository.save(paymentMapper.toEntity(payment));
            
            // Create Stripe PaymentIntent
            Map<String, String> metadata = new HashMap<>();
            metadata.put("type", "B2C_ORDER");
            metadata.put("orderId", orderId.toString());
            if (userId != null) {
                metadata.put("userId", userId.toString());
            }
            metadata.put("paymentId", savedPayment.getId().toString());
            if (idempotencyKey != null) {
                metadata.put("idempotencyKey", idempotencyKey);
            }
            
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountToStripeFormat(amount, currency))
                    .setCurrency(currency.toLowerCase())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .putAllMetadata(metadata)
                    .setReceiptEmail(customerEmail)
                    .build();
            
            PaymentIntent intent = PaymentIntent.create(params);
            
            // Update payment with Stripe PaymentIntent ID
            savedPayment.setStripePaymentIntentId(intent.getId());
            savedPayment.setStatus(PaymentStatus.REQUIRES_ACTION);
            paymentRepository.save(savedPayment);
            
            log.info("PaymentIntent created: paymentIntentId={}, paymentId={}",
                    intent.getId(), savedPayment.getId());
            
            return new PaymentIntentResponse(
                    savedPayment.getId(),
                    intent.getId(),
                    intent.getClientSecret(),
                    amount,
                    currency,
                    intent.getStatus(),
                    stripeConfig.getPublishableKey(),
                    orderId
            );
            
        } catch (StripeException e) {
            log.error("Stripe error creating PaymentIntent: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.STRIPE_ERROR,
                    "Failed to create payment intent: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create PaymentIntent for vendor wallet top-up
     */
    @Transactional
    public PaymentIntentResponse createPaymentIntentForTopUp(Long vendorId, BigDecimal amount,
                                                             String currency, String idempotencyKey) {
        log.info("Creating PaymentIntent for vendor top-up: vendorId={}, amount={}",
                vendorId, amount);
        
        try {
            // Create payment record
            Payment payment = Payment.builder()
                    .paymentNumber(generatePaymentNumber())
                    .type(PaymentType.VENDOR_TOPUP)
                    .vendorId(vendorId)
                    .amount(amount)
                    .refundedAmount(BigDecimal.ZERO)
                    .currency(currency.toUpperCase())
                    .status(PaymentStatus.CREATED)
                    .idempotencyKey(idempotencyKey)
                    .build();
            
            PaymentJpaEntity savedPayment = paymentRepository.save(paymentMapper.toEntity(payment));
            
            // Create Stripe PaymentIntent
            Map<String, String> metadata = new HashMap<>();
            metadata.put("type", "VENDOR_TOPUP");
            metadata.put("vendorId", vendorId.toString());
            metadata.put("paymentId", savedPayment.getId().toString());
            if (idempotencyKey != null) {
                metadata.put("idempotencyKey", idempotencyKey);
            }
            
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountToStripeFormat(amount, currency))
                    .setCurrency(currency.toLowerCase())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .putAllMetadata(metadata)
                    .build();
            
            PaymentIntent intent = PaymentIntent.create(params);
            
            // Update payment with Stripe PaymentIntent ID
            savedPayment.setStripePaymentIntentId(intent.getId());
            savedPayment.setStatus(PaymentStatus.REQUIRES_ACTION);
            paymentRepository.save(savedPayment);
            
            log.info("PaymentIntent created for top-up: paymentIntentId={}, paymentId={}",
                    intent.getId(), savedPayment.getId());
            
            return new PaymentIntentResponse(
                    savedPayment.getId(),
                    intent.getId(),
                    intent.getClientSecret(),
                    amount,
                    currency,
                    intent.getStatus(),
                    stripeConfig.getPublishableKey(),
                    null // No orderId for vendor top-up
            );
            
        } catch (StripeException e) {
            log.error("Stripe error creating PaymentIntent for top-up: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.STRIPE_ERROR,
                    "Failed to create payment intent: " + e.getMessage(), e);
        }
    }
    
    /**
     * Process payment success (called from webhook)
     */
    @Transactional
    public Payment processPaymentSuccess(String paymentIntentId, Map<String, Object> stripeMetadata) {
        log.info("Processing payment success: paymentIntentId={}", paymentIntentId);
        
        PaymentJpaEntity payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND,
                        "Payment not found for PaymentIntent: " + paymentIntentId));
        
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            log.warn("Payment already succeeded: paymentId={}", payment.getId());
            return paymentMapper.toDomain(payment);
        }
        
        // Update payment status
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setSucceededAt(LocalDateTime.now());
        
        // Update metadata from Stripe
        if (stripeMetadata != null) {
            payment.setMetadata(stripeMetadata);
        }
        
        PaymentJpaEntity saved = paymentRepository.save(payment);
        log.info("Payment marked as succeeded: paymentId={}, type={}", saved.getId(), saved.getType());
        
        return paymentMapper.toDomain(saved);
    }
    
    /**
     * Process payment failure (called from webhook)
     */
    @Transactional
    public Payment processPaymentFailure(String paymentIntentId, String errorMessage) {
        log.info("Processing payment failure: paymentIntentId={}", paymentIntentId);
        
        PaymentJpaEntity payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND,
                        "Payment not found for PaymentIntent: " + paymentIntentId));
        
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailedAt(LocalDateTime.now());
        payment.setErrorMessage(errorMessage);
        
        PaymentJpaEntity saved = paymentRepository.save(payment);
        log.info("Payment marked as failed: paymentId={}", saved.getId());
        
        return paymentMapper.toDomain(saved);
    }
    
    /**
     * Create refund
     */
    @Transactional
    public Payment createRefund(Long paymentId, BigDecimal amount, String reason) {
        log.info("Creating refund: paymentId={}, amount={}", paymentId, amount);
        
        PaymentJpaEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND,
                        "Payment not found: " + paymentId));
        
        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED,
                    "Payment must be successful to refund");
        }
        
        BigDecimal refundableAmount = payment.getAmount().subtract(payment.getRefundedAmount());
        if (amount.compareTo(refundableAmount) > 0) {
            throw new BusinessException(ErrorCode.REFUND_AMOUNT_EXCEEDS_PAYMENT,
                    String.format("Refund amount %s exceeds refundable amount %s",
                            amount, refundableAmount));
        }
        
        try {
            // Create Stripe refund
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(payment.getStripePaymentIntentId())
                    .setAmount(amountToStripeFormat(amount, payment.getCurrency()))
                    .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                    .putMetadata("paymentId", paymentId.toString())
                    .putMetadata("reason", reason != null ? reason : "Customer requested")
                    .build();
            
            Refund refund = Refund.create(params);
            
            // Update payment
            payment.setRefundedAmount(payment.getRefundedAmount().add(amount));
            payment.setStripeRefundId(refund.getId());
            payment.setRefundedAt(LocalDateTime.now());
            
            // Update status
            if (payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0) {
                payment.setStatus(PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }
            
            PaymentJpaEntity saved = paymentRepository.save(payment);
            log.info("Refund created: refundId={}, paymentId={}, amount={}",
                    refund.getId(), paymentId, amount);
            
            return paymentMapper.toDomain(saved);
            
        } catch (StripeException e) {
            log.error("Stripe error creating refund: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.REFUND_FAILED,
                    "Failed to create refund: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get payment by ID
     */
    public Payment getPaymentById(Long paymentId) {
        PaymentJpaEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND,
                        "Payment not found: " + paymentId));
        return paymentMapper.toDomain(payment);
    }
    
    /**
     * Get payment by order ID
     */
    public Payment getPaymentByOrderId(Long orderId) {
        PaymentJpaEntity payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND,
                        "Payment not found for order: " + orderId));
        return paymentMapper.toDomain(payment);
    }
    
    /**
     * Convert amount to Stripe format (smallest currency unit)
     */
    private Long amountToStripeFormat(BigDecimal amount, String currency) {
        // Most currencies use 2 decimal places (multiply by 100)
        // Zero-decimal currencies (JPY, KRW, etc.) don't need multiplication
        boolean isZeroDecimal = isZeroDecimalCurrency(currency);
        
        if (isZeroDecimal) {
            return amount.longValue();
        } else {
            return amount.multiply(BigDecimal.valueOf(100)).longValue();
        }
    }
    
    private boolean isZeroDecimalCurrency(String currency) {
        return currency.equalsIgnoreCase("JPY") ||
               currency.equalsIgnoreCase("KRW") ||
               currency.equalsIgnoreCase("VND") ||
               currency.equalsIgnoreCase("CLP");
    }
    
    private String generatePaymentNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "PAY-" + timestamp + "-" + uuid;
    }
    
    /**
     * Response DTO for PaymentIntent creation
     */
    public record PaymentIntentResponse(
            Long paymentId,
            String paymentIntentId,
            String clientSecret,
            BigDecimal amount,
            String currency,
            String status,
            String publishableKey,
            Long orderId // null for vendor top-up, orderId for B2C orders
    ) {}
}

