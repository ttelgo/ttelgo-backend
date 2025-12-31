package com.tiktel.ttelgo.order.application;

import com.tiktel.ttelgo.common.domain.enums.OrderStatus;
import com.tiktel.ttelgo.common.domain.enums.PaymentStatus;
import com.tiktel.ttelgo.common.exception.BusinessException;
import com.tiktel.ttelgo.common.exception.ErrorCode;
import com.tiktel.ttelgo.common.exception.ResourceNotFoundException;
import com.tiktel.ttelgo.integration.esimgo.EsimGoService;
import com.tiktel.ttelgo.integration.esimgo.domain.Bundle;
import com.tiktel.ttelgo.integration.esimgo.domain.OrderResult;
import com.tiktel.ttelgo.order.api.dto.OrderResponse;
import com.tiktel.ttelgo.order.api.mapper.OrderApiMapper;
import com.tiktel.ttelgo.order.domain.Order;
import com.tiktel.ttelgo.order.infrastructure.mapper.OrderMapper;
import com.tiktel.ttelgo.order.infrastructure.repository.OrderJpaEntity;
import com.tiktel.ttelgo.order.infrastructure.repository.OrderRepository;
import com.tiktel.ttelgo.vendor.application.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Order service with state machine and provisioning logic
 */
@Slf4j
@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderApiMapper orderApiMapper;
    private final EsimGoService esimGoService;
    private final VendorService vendorService;
    
    public OrderService(OrderRepository orderRepository,
                       OrderMapper orderMapper,
                       OrderApiMapper orderApiMapper,
                       EsimGoService esimGoService,
                       VendorService vendorService) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.orderApiMapper = orderApiMapper;
        this.esimGoService = esimGoService;
        this.vendorService = vendorService;
    }
    
    /**
     * Get order by ID
     */
    public Order getOrderById(Long orderId) {
        OrderJpaEntity entity = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND,
                        "Order not found with ID: " + orderId));
        return orderMapper.toDomain(entity);
    }
    
    /**
     * Get order by ID as OrderResponse
     */
    public OrderResponse getOrderResponseById(Long orderId) {
        Order order = getOrderById(orderId);
        return orderApiMapper.toResponse(order);
    }
    
    /**
     * Get order by order number
     */
    public Order getOrderByNumber(String orderNumber) {
        OrderJpaEntity entity = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND,
                        "Order not found with number: " + orderNumber));
        return orderMapper.toDomain(entity);
    }
    
    /**
     * Get order by reference (order number)
     */
    public OrderResponse getOrderByReference(String reference) {
        Order order = getOrderByNumber(reference);
        return orderApiMapper.toResponse(order);
    }
    
    /**
     * Get orders by user ID as list of OrderResponse
     */
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        List<OrderJpaEntity> entities = orderRepository.findByUserId(userId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        return entities.stream()
                .map(orderMapper::toDomain)
                .map(orderApiMapper::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get orders for user (B2C)
     */
    public Page<Order> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(orderMapper::toDomain);
    }
    
    /**
     * Get orders for vendor (B2B)
     */
    public Page<Order> getVendorOrders(Long vendorId, Pageable pageable) {
        return orderRepository.findByVendorId(vendorId, pageable)
                .map(orderMapper::toDomain);
    }
    
    /**
     * Search orders with filters
     */
    public Page<Order> searchOrders(Long userId, Long vendorId, OrderStatus status,
                                    LocalDateTime startDate, LocalDateTime endDate,
                                    Pageable pageable) {
        return orderRepository.findByFilters(userId, vendorId, status, startDate, endDate, pageable)
                .map(orderMapper::toDomain);
    }
    
    /**
     * Create B2C order (customer order)
     */
    @Transactional
    public Order createB2COrder(Long userId, String customerEmail, String bundleCode,
                                int quantity, String ipAddress, String userAgent) {
        log.info("Creating B2C order: userId={}, bundleCode={}, quantity={}", 
                userId, bundleCode, quantity);
        
        // Get bundle details from eSIM Go
        Bundle bundle = esimGoService.getBundleDetails(bundleCode);
        if (bundle == null) {
            throw new BusinessException(ErrorCode.BUNDLE_NOT_FOUND,
                    "Bundle not found: " + bundleCode);
        }
        
        if (!Boolean.TRUE.equals(bundle.getAvailable())) {
            throw new BusinessException(ErrorCode.BUNDLE_NOT_AVAILABLE,
                    "Bundle is not available: " + bundleCode);
        }
        
        // Calculate pricing
        BigDecimal unitPrice = bundle.getPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        
        // Create order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .userId(userId)
                .customerEmail(customerEmail)
                .bundleCode(bundleCode)
                .bundleName(bundle.getName())
                .quantity(quantity)
                .unitPrice(unitPrice)
                .totalAmount(totalAmount)
                .currency(bundle.getCurrency())
                .status(OrderStatus.ORDER_CREATED)
                .paymentStatus(PaymentStatus.CREATED)
                .countryIso(bundle.getCountryCode())
                .dataAmount(bundle.getDataAmount())
                .validityDays(bundle.getValidityDays())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .retryCount(0)
                .build();
        
        OrderJpaEntity entity = orderMapper.toEntity(order);
        OrderJpaEntity saved = orderRepository.save(entity);
        
        log.info("B2C order created: orderId={}, orderNumber={}, totalAmount={}",
                saved.getId(), saved.getOrderNumber(), totalAmount);
        
        return orderMapper.toDomain(saved);
    }
    
    /**
     * Create B2B order (vendor order)
     */
    @Transactional
    public Order createB2BOrder(Long vendorId, String bundleCode, int quantity,
                               String ipAddress, String userAgent) {
        log.info("Creating B2B order: vendorId={}, bundleCode={}, quantity={}",
                vendorId, bundleCode, quantity);
        
        // Get bundle details
        Bundle bundle = esimGoService.getBundleDetails(bundleCode);
        if (bundle == null) {
            throw new BusinessException(ErrorCode.BUNDLE_NOT_FOUND,
                    "Bundle not found: " + bundleCode);
        }
        
        // Calculate pricing
        BigDecimal unitPrice = bundle.getPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        
        // Validate vendor can place order
        vendorService.validateVendorCanPlaceOrder(vendorId, totalAmount);
        
        // Debit vendor immediately (PREPAID) or add to outstanding (POSTPAID)
        vendorService.debitForOrder(vendorId, totalAmount, null,
                "Order payment: " + bundleCode + " x " + quantity, null);
        
        // Create order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .vendorId(vendorId)
                .bundleCode(bundleCode)
                .bundleName(bundle.getName())
                .quantity(quantity)
                .unitPrice(unitPrice)
                .totalAmount(totalAmount)
                .currency(bundle.getCurrency())
                .status(OrderStatus.PAID) // Vendor orders are pre-paid
                .paymentStatus(PaymentStatus.SUCCEEDED)
                .countryIso(bundle.getCountryCode())
                .dataAmount(bundle.getDataAmount())
                .validityDays(bundle.getValidityDays())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .retryCount(0)
                .build();
        
        OrderJpaEntity entity = orderMapper.toEntity(order);
        OrderJpaEntity saved = orderRepository.save(entity);
        
        // Update ledger entry with order ID
        vendorService.debitForOrder(vendorId, totalAmount, saved.getId(),
                "Order payment: " + bundleCode + " x " + quantity, null);
        
        log.info("B2B order created: orderId={}, orderNumber={}, totalAmount={}",
                saved.getId(), saved.getOrderNumber(), totalAmount);
        
        // Start provisioning immediately for B2B orders
        try {
            provisionOrder(saved.getId());
        } catch (Exception e) {
            log.error("Failed to provision B2B order immediately: orderId={}", saved.getId(), e);
            // Order is still created, will be retried by reconciliation job
        }
        
        return orderMapper.toDomain(saved);
    }
    
    /**
     * Mark order as paid (after payment confirmation)
     */
    @Transactional
    public Order markOrderAsPaid(Long orderId, Long paymentId) {
        log.info("Marking order as paid: orderId={}, paymentId={}", orderId, paymentId);
        
        OrderJpaEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND,
                        "Order not found: " + orderId));
        
        if (order.getStatus() != OrderStatus.ORDER_CREATED &&
            order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS,
                    "Order cannot be marked as paid in current status: " + order.getStatus());
        }
        
        order.setStatus(OrderStatus.PAID);
        order.setPaymentStatus(PaymentStatus.SUCCEEDED);
        order.setPaidAt(LocalDateTime.now());
        
        OrderJpaEntity saved = orderRepository.save(order);
        log.info("Order marked as paid: orderId={}", orderId);
        
        // Start provisioning
        try {
            provisionOrder(orderId);
        } catch (Exception e) {
            log.error("Failed to provision order after payment: orderId={}", orderId, e);
            // Order is still paid, will be retried by reconciliation job
        }
        
        return orderMapper.toDomain(saved);
    }
    
    /**
     * Provision order with eSIM Go
     */
    @Transactional
    public Order provisionOrder(Long orderId) {
        log.info("Provisioning order with eSIM Go: orderId={}", orderId);
        
        OrderJpaEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND,
                        "Order not found: " + orderId));
        
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS,
                    "Order must be paid before provisioning: " + order.getStatus());
        }
        
        if (order.getProvisionedAt() != null) {
            log.warn("Order already provisioned: orderId={}", orderId);
            return orderMapper.toDomain(order);
        }
        
        try {
            // Update status to provisioning
            order.setStatus(OrderStatus.PROVISIONING);
            orderRepository.save(order);
            
            // Call eSIM Go to create order
            OrderResult result = esimGoService.createOrder(order.getBundleCode(), order.getQuantity());
            
            // Update order with eSIM Go reference
            order.setEsimgoOrderId(result.getOrderId());
            order.setProvisionedAt(LocalDateTime.now());
            order.setStatus(OrderStatus.COMPLETED);
            order.setCompletedAt(LocalDateTime.now());
            
            OrderJpaEntity saved = orderRepository.save(order);
            log.info("Order provisioned successfully: orderId={}, esimgoOrderId={}",
                    orderId, result.getOrderId());
            
            // TODO: Create eSIM records in database
            // TODO: Send notification to customer/vendor
            
            return orderMapper.toDomain(saved);
            
        } catch (BusinessException e) {
            log.error("eSIM Go error provisioning order: orderId={}", orderId, e);
            
            order.setStatus(OrderStatus.SYNC_FAILED);
            order.setErrorCode(e.getErrorCode().getCode());
            order.setErrorMessage(e.getMessage());
            order.setRetryCount(order.getRetryCount() + 1);
            order.setLastRetryAt(LocalDateTime.now());
            orderRepository.save(order);
            
            throw e;
            
        } catch (Exception e) {
            log.error("Unexpected error provisioning order: orderId={}", orderId, e);
            
            order.setStatus(OrderStatus.FAILED);
            order.setErrorMessage(e.getMessage());
            order.setFailedAt(LocalDateTime.now());
            orderRepository.save(order);
            
            throw new BusinessException(ErrorCode.ORDER_PROVISIONING_FAILED,
                    "Failed to provision order", e);
        }
    }
    
    /**
     * Cancel order
     */
    @Transactional
    public Order cancelOrder(Long orderId, String reason) {
        log.info("Canceling order: orderId={}, reason={}", orderId, reason);
        
        OrderJpaEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND,
                        "Order not found: " + orderId));
        
        Order domainOrder = orderMapper.toDomain(order);
        if (!domainOrder.canBeCanceled()) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_BE_CANCELED,
                    "Order cannot be canceled in current status: " + order.getStatus());
        }
        
        order.setStatus(OrderStatus.CANCELED);
        order.setCanceledAt(LocalDateTime.now());
        order.setErrorMessage(reason);
        
        OrderJpaEntity saved = orderRepository.save(order);
        log.info("Order canceled: orderId={}", orderId);
        
        // TODO: Refund payment if applicable
        // TODO: Refund vendor if B2B order
        
        return orderMapper.toDomain(saved);
    }
    
    /**
     * Retry failed order provisioning
     */
    @Transactional
    public Order retryProvisioning(Long orderId) {
        log.info("Retrying order provisioning: orderId={}", orderId);
        
        OrderJpaEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND,
                        "Order not found: " + orderId));
        
        if (order.getStatus() != OrderStatus.SYNC_FAILED &&
            order.getStatus() != OrderStatus.FAILED) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS,
                    "Order cannot be retried in current status: " + order.getStatus());
        }
        
        // Reset to PAID status
        order.setStatus(OrderStatus.PAID);
        order.setErrorCode(null);
        order.setErrorMessage(null);
        orderRepository.save(order);
        
        // Retry provisioning
        return provisionOrder(orderId);
    }
    
    /**
     * Find stale orders that need reconciliation
     */
    public List<Order> findStaleOrders(int minutesOld) {
        LocalDateTime beforeTime = LocalDateTime.now().minusMinutes(minutesOld);
        List<OrderStatus> statuses = List.of(
                OrderStatus.PAYMENT_PENDING,
                OrderStatus.PROVISIONING,
                OrderStatus.SYNC_FAILED
        );
        
        return orderRepository.findStaleOrders(statuses, beforeTime).stream()
                .map(orderMapper::toDomain)
                .toList();
    }
    
    /**
     * Generate unique order number
     */
    private String generateOrderNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-" + timestamp + "-" + uuid;
    }
}
