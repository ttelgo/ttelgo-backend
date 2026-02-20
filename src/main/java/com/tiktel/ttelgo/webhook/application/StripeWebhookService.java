package com.tiktel.ttelgo.webhook.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.tiktel.ttelgo.common.domain.enums.PaymentType;
import com.tiktel.ttelgo.common.exception.BusinessException;
import com.tiktel.ttelgo.common.exception.ErrorCode;
import com.tiktel.ttelgo.integration.stripe.StripeConfig;
import com.tiktel.ttelgo.payment.infrastructure.adapter.StripeService;
import com.tiktel.ttelgo.order.application.OrderService;
import com.tiktel.ttelgo.payment.domain.Payment;
import com.tiktel.ttelgo.vendor.application.VendorService;
import com.tiktel.ttelgo.webhook.infrastructure.repository.WebhookEventJpaEntity;
import com.tiktel.ttelgo.webhook.infrastructure.repository.WebhookEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for handling incoming Stripe webhooks
 */
@Slf4j
@Service
public class StripeWebhookService {
    
    private final WebhookEventRepository webhookEventRepository;
    private final StripeService stripeService;
    private final OrderService orderService;
    private final VendorService vendorService;
    private final StripeConfig stripeConfig;
    private final ObjectMapper objectMapper;
    
    public StripeWebhookService(WebhookEventRepository webhookEventRepository,
                               StripeService stripeService,
                               OrderService orderService,
                               VendorService vendorService,
                               StripeConfig stripeConfig,
                               ObjectMapper objectMapper) {
        this.webhookEventRepository = webhookEventRepository;
        this.stripeService = stripeService;
        this.orderService = orderService;
        this.vendorService = vendorService;
        this.stripeConfig = stripeConfig;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Process Stripe webhook event
     */
    @Transactional
    public void processWebhook(String payload, String signatureHeader) {
        log.info("Received Stripe webhook");
        
        // Verify signature
        Event event;
        try {
            event = Webhook.constructEvent(
                    payload,
                    signatureHeader,
                    stripeConfig.getWebhookSecret()
            );
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe webhook signature", e);
            throw new BusinessException(ErrorCode.WEBHOOK_SIGNATURE_INVALID,
                    "Invalid webhook signature", e);
        }
        
        // Check for duplicate
        if (webhookEventRepository.existsBySourceAndEventId("STRIPE", event.getId())) {
            log.info("Duplicate Stripe webhook event ignored: eventId={}", event.getId());
            return;
        }
        
        // Store webhook event
        Map<String, Object> payloadMap = objectMapper.convertValue(event, Map.class);
        WebhookEventJpaEntity webhookEvent = WebhookEventJpaEntity.builder()
                .source("STRIPE")
                .eventId(event.getId())
                .eventType(event.getType())
                .payload(payloadMap)
                .processed(false)
                .processingAttempts(0)
                .build();
        
        webhookEventRepository.save(webhookEvent);
        
        // Process event
        try {
            processStripeEvent(event, webhookEvent);
            
            webhookEvent.setProcessed(true);
            webhookEvent.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(webhookEvent);
            
            log.info("Stripe webhook processed successfully: eventId={}, type={}",
                    event.getId(), event.getType());
            
        } catch (Exception e) {
            log.error("Error processing Stripe webhook: eventId={}, type={}",
                    event.getId(), event.getType(), e);
            
            webhookEvent.setProcessingAttempts(webhookEvent.getProcessingAttempts() + 1);
            webhookEvent.setLastProcessingAttemptAt(LocalDateTime.now());
            webhookEvent.setProcessingError(e.getMessage());
            webhookEventRepository.save(webhookEvent);
            
            throw new BusinessException(ErrorCode.WEBHOOK_PROCESSING_FAILED,
                    "Failed to process webhook", e);
        }
    }
    
    /**
     * Process individual Stripe event
     */
    private void processStripeEvent(Event event, WebhookEventJpaEntity webhookEvent) {
        StripeObject stripeObject = event.getDataObjectDeserializer().getObject()
                .orElse(null);
        
        if (!(stripeObject instanceof PaymentIntent)) {
            log.info("Ignoring non-PaymentIntent event: type={}", event.getType());
            return;
        }
        
        PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
        Map<String, String> metadata = paymentIntent.getMetadata();
        
        switch (event.getType()) {
            case "payment_intent.succeeded":
                handlePaymentSuccess(paymentIntent, metadata, webhookEvent);
                break;
                
            case "payment_intent.payment_failed":
                handlePaymentFailure(paymentIntent, metadata, webhookEvent);
                break;
                
            case "charge.refunded":
            case "refund.created":
                handleRefund(paymentIntent, metadata, webhookEvent);
                break;
                
            default:
                log.info("Unhandled Stripe event type: {}", event.getType());
        }
    }
    
    /**
     * Handle payment success
     */
    private void handlePaymentSuccess(PaymentIntent paymentIntent,
                                      Map<String, String> metadata,
                                      WebhookEventJpaEntity webhookEvent) {
        log.info("Processing payment success: paymentIntentId={}", paymentIntent.getId());
        
        // Update payment
        Map<String, Object> metadataMap = new HashMap<>(metadata);
        Payment payment = stripeService.processPaymentSuccess(paymentIntent.getId(), metadataMap);
        webhookEvent.setPaymentId(payment.getId());
        
        // Determine type and process accordingly
        String type = metadata.get("type");
        if ("B2C_ORDER".equals(type)) {
            handleB2COrderPaymentSuccess(payment, metadata, webhookEvent);
        } else if ("VENDOR_TOPUP".equals(type)) {
            handleVendorTopUpSuccess(payment, metadata, webhookEvent);
        }
    }
    
    /**
     * Handle B2C order payment success
     */
    private void handleB2COrderPaymentSuccess(Payment payment,
                                              Map<String, String> metadata,
                                              WebhookEventJpaEntity webhookEvent) {
        if (payment.getOrderId() != null) {
            log.info("Marking order as paid: orderId={}", payment.getOrderId());
            orderService.markOrderAsPaid(payment.getOrderId(), payment.getId());
            webhookEvent.setOrderId(payment.getOrderId());
        }
    }
    
    /**
     * Handle vendor wallet top-up success
     */
    private void handleVendorTopUpSuccess(Payment payment,
                                          Map<String, String> metadata,
                                          WebhookEventJpaEntity webhookEvent) {
        if (payment.getVendorId() != null) {
            log.info("Processing vendor top-up: vendorId={}, amount={}",
                    payment.getVendorId(), payment.getAmount());
            
            vendorService.topUpWallet(
                    payment.getVendorId(),
                    payment.getAmount(),
                    payment.getId(),
                    "Wallet top-up via Stripe",
                    null
            );
        }
    }
    
    /**
     * Handle payment failure
     */
    private void handlePaymentFailure(PaymentIntent paymentIntent,
                                      Map<String, String> metadata,
                                      WebhookEventJpaEntity webhookEvent) {
        log.info("Processing payment failure: paymentIntentId={}", paymentIntent.getId());
        
        String errorMessage = paymentIntent.getLastPaymentError() != null ?
                paymentIntent.getLastPaymentError().getMessage() : "Payment failed";
        
        Payment payment = stripeService.processPaymentFailure(paymentIntent.getId(), errorMessage);
        webhookEvent.setPaymentId(payment.getId());
        
        // TODO: Notify customer/vendor about payment failure
    }
    
    /**
     * Handle refund
     */
    private void handleRefund(PaymentIntent paymentIntent,
                              Map<String, String> metadata,
                              WebhookEventJpaEntity webhookEvent) {
        log.info("Processing refund: paymentIntentId={}", paymentIntent.getId());
        
        // Payment status is already updated by Stripe
        // Additional business logic can be added here
        
        // TODO: Notify customer/vendor about refund
        // TODO: Reverse vendor debit if it was a B2B order
    }
}

