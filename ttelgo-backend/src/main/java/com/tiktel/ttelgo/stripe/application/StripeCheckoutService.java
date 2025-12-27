package com.tiktel.ttelgo.stripe.application;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.tiktel.ttelgo.order.domain.Order;
import com.tiktel.ttelgo.order.domain.OrderStatus;
import com.tiktel.ttelgo.order.domain.PaymentStatus;
import com.tiktel.ttelgo.order.infrastructure.repository.OrderRepository;
import com.tiktel.ttelgo.payment.domain.Payment;
import com.tiktel.ttelgo.payment.infrastructure.repository.PaymentRepository;
import com.tiktel.ttelgo.plan.application.PlanService;
import com.tiktel.ttelgo.plan.api.dto.ListBundlesResponse;
import com.tiktel.ttelgo.stripe.api.dto.CreateCheckoutSessionRequest;
import com.tiktel.ttelgo.stripe.api.dto.CreateCheckoutSessionResponse;
import com.tiktel.ttelgo.stripe.api.dto.SessionDetailsResponse;
import com.tiktel.ttelgo.stripe.config.AppProperties;
import com.tiktel.ttelgo.stripe.config.StripeProperties;
import com.tiktel.ttelgo.stripe.domain.StripeWebhookEvent;
import com.tiktel.ttelgo.stripe.infrastructure.repository.StripeWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeCheckoutService {

    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("29.99");

    private final StripeProperties stripeProperties;
    private final AppProperties appProperties;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final StripeWebhookEventRepository webhookEventRepository;
    private final PlanService planService;

    @Transactional
    public CreateCheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request) throws StripeException {
        ensureStripeKey();

        Order order = resolveOrder(request);
        long amountCents = toCents(order.getTotalAmount());

        SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                .setQuantity((long) Math.max(1, Optional.ofNullable(request.getQuantity()).orElse(1)))
                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(order.getCurrency().toLowerCase(Locale.ROOT))
                        .setUnitAmount(amountCents / Math.max(1, Optional.ofNullable(request.getQuantity()).orElse(1)))
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                .setName(order.getBundleName() != null ? order.getBundleName() : "eSIM bundle")
                                .build())
                        .build())
                .build();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(appProperties.getFrontendUrl() + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(appProperties.getFrontendUrl() + "/payment/cancel")
                .addLineItem(lineItem)
                .putMetadata("orderId", String.valueOf(order.getId()))
                .build();

        Session session = Session.create(params);

        Payment payment = paymentRepository.findByStripeSessionId(session.getId())
                .orElseGet(Payment::new);
        payment.setOrderId(order.getId());
        payment.setPaymentIntentId(session.getPaymentIntent());
        payment.setStripeSessionId(session.getId());
        payment.setCustomerId(session.getCustomer());
        payment.setCustomerEmail(request.getCustomerEmail());
        payment.setAmount(order.getTotalAmount());
        payment.setCurrency(order.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod("stripe_checkout");
        payment.setCreatedAt(Optional.ofNullable(payment.getCreatedAt()).orElse(LocalDateTime.now()));
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.PENDING);
        orderRepository.save(order);

        return CreateCheckoutSessionResponse.builder()
                .sessionId(session.getId())
                .url(session.getUrl())
                .orderId(order.getId())
                .paymentStatus(payment.getStatus().name())
                .build();
    }

    public SessionDetailsResponse getSessionDetails(String sessionId) throws StripeException {
        ensureStripeKey();
        Session session = Session.retrieve(sessionId);

        return SessionDetailsResponse.builder()
                .sessionId(session.getId())
                .status(session.getStatus())
                .paymentStatus(session.getPaymentStatus())
                .amountTotal(session.getAmountTotal())
                .currency(session.getCurrency())
                .paymentIntentId(session.getPaymentIntent())
                .customerEmail(session.getCustomerDetails() != null ? session.getCustomerDetails().getEmail() : null)
                .metadata(session.getMetadata())
                .build();
    }

    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        ensureStripeKey();
        if (!StringUtils.hasText(stripeProperties.getWebhookSecret())) {
            log.warn("Stripe webhook secret is not configured; skipping webhook processing.");
            return;
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, stripeProperties.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.error("Stripe webhook signature verification failed", e);
            throw new IllegalArgumentException("Invalid signature");
        }

        if (webhookEventRepository.existsById(event.getId())) {
            log.info("Webhook event {} already processed, skipping.", event.getId());
            return;
        }

        StripeWebhookEvent webhookEvent = new StripeWebhookEvent();
        webhookEvent.setEventId(event.getId());
        webhookEventRepository.save(webhookEvent);

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "checkout.session.expired" -> handleCheckoutExpired(event);
            case "payment_intent.payment_failed" -> handlePaymentFailed(event);
            default -> log.info("Unhandled event type: {}", event.getType());
        }
    }

    private void handleCheckoutCompleted(Event event) {
        Session session = extractSession(event);
        if (session == null) {
            log.warn("checkout.session.completed missing session object");
            return;
        }

        Optional<Payment> paymentOpt = paymentRepository.findByStripeSessionId(session.getId());
        paymentOpt.ifPresent(payment -> {
            payment.setPaymentIntentId(session.getPaymentIntent());
            payment.setCustomerEmail(session.getCustomerDetails() != null ? session.getCustomerDetails().getEmail() : payment.getCustomerEmail());
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
        });

        String orderIdStr = session.getMetadata() != null ? session.getMetadata().get("orderId") : null;
        if (orderIdStr != null) {
            try {
                Long orderId = Long.parseLong(orderIdStr);
                orderRepository.findById(orderId).ifPresent(order -> {
                    order.setPaymentStatus(PaymentStatus.SUCCESS);
                    order.setStatus(OrderStatus.COMPLETED);
                    orderRepository.save(order);
                });
            } catch (NumberFormatException e) {
                log.warn("Invalid orderId in metadata: {}", orderIdStr);
            }
        }
    }

    private void handleCheckoutExpired(Event event) {
        Session session = extractSession(event);
        if (session == null) {
            return;
        }
        paymentRepository.findByStripeSessionId(session.getId()).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.EXPIRED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
        });

        String orderIdStr = session.getMetadata() != null ? session.getMetadata().get("orderId") : null;
        if (orderIdStr != null) {
            try {
                Long orderId = Long.parseLong(orderIdStr);
                orderRepository.findById(orderId).ifPresent(order -> {
                    order.setPaymentStatus(PaymentStatus.EXPIRED);
                    order.setStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);
                });
            } catch (NumberFormatException e) {
                log.warn("Invalid orderId in metadata: {}", orderIdStr);
            }
        }
    }

    private void handlePaymentFailed(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = deserializer.getObject().orElse(null);
        if (stripeObject instanceof PaymentIntent paymentIntent) {
            String intentId = paymentIntent.getId();
            paymentRepository.findByPaymentIntentId(intentId).ifPresent(payment -> {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(paymentIntent.getLastPaymentError() != null
                        ? paymentIntent.getLastPaymentError().getMessage()
                        : "Payment failed");
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
            });

            String orderIdStr = paymentIntent.getMetadata() != null ? paymentIntent.getMetadata().get("orderId") : null;
            if (orderIdStr != null) {
                try {
                    Long orderId = Long.parseLong(orderIdStr);
                    orderRepository.findById(orderId).ifPresent(order -> {
                        order.setPaymentStatus(PaymentStatus.FAILED);
                        order.setStatus(OrderStatus.FAILED);
                        orderRepository.save(order);
                    });
                } catch (NumberFormatException e) {
                    log.warn("Invalid orderId in payment intent metadata: {}", orderIdStr);
                }
            }
        }
    }

    private Session extractSession(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = deserializer.getObject().orElse(null);
        if (stripeObject instanceof Session session) {
            return session;
        }
        return null;
    }

    private Order resolveOrder(CreateCheckoutSessionRequest request) {
        if (request.getOrderId() != null) {
            Order existing = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + request.getOrderId()));
            if (existing.getTotalAmount() == null || existing.getCurrency() == null) {
                throw new IllegalStateException("Order is missing amount or currency; cannot create checkout session.");
            }
            return existing;
        }

        BigDecimal unitPrice = resolvePrice(request.getBundleId(), request.getBundleName());
        int qty = Math.max(1, Optional.ofNullable(request.getQuantity()).orElse(1));
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(qty));
        String currency = StringUtils.hasText(request.getCurrency()) ? request.getCurrency().toLowerCase(Locale.ROOT) : "usd";

        Order order = new Order();
        order.setOrderReference(UUID.randomUUID().toString());
        order.setBundleId(request.getBundleId());
        order.setBundleName(request.getBundleName());
        order.setPackageName(Optional.ofNullable(request.getBundleName()).orElse("eSIM package"));
        order.setQuantity(qty);
        order.setUnitPrice(unitPrice);
        order.setTotalAmount(total);
        order.setCurrency(currency);
        Long userId = request.getUserId() != null ? request.getUserId() : 31L; // fallback to provided default user
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        return orderRepository.save(order);
    }

    private BigDecimal resolvePrice(String bundleId, String bundleName) {
        try {
            String key = StringUtils.hasText(bundleId) ? bundleId : bundleName;
            if (StringUtils.hasText(key)) {
                ListBundlesResponse.BundleDto bundle = planService.getBundleDetails(key);
                if (bundle != null && bundle.getPrice() != null && bundle.getPrice() > 0) {
                    return BigDecimal.valueOf(bundle.getPrice());
                }
            }
        } catch (Exception e) {
            log.warn("Unable to resolve price from PlanService, falling back to local catalog. Reason: {}", e.getMessage());
        }

        String lookupKey = Optional.ofNullable(bundleName).orElse(bundleId);
        return devCatalogPrice(lookupKey);
    }

    private BigDecimal devCatalogPrice(String key) {
        Map<String, BigDecimal> catalog = Map.of(
                "1", DEFAULT_PRICE,
                "global_esim_plan", DEFAULT_PRICE,
                "global-esim-plan", DEFAULT_PRICE,
                "default", DEFAULT_PRICE
        );
        return catalog.getOrDefault(
                Optional.ofNullable(key).map(k -> k.toLowerCase(Locale.ROOT)).orElse("default"),
                DEFAULT_PRICE
        );
    }

    private long toCents(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private void ensureStripeKey() {
        if (!StringUtils.hasText(stripeProperties.getSecretKey())) {
            throw new IllegalStateException("STRIPE_SECRET_KEY is not configured");
        }
        Stripe.apiKey = stripeProperties.getSecretKey();
    }
}

