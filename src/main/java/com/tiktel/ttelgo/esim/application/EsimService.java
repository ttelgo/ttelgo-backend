package com.tiktel.ttelgo.esim.application;

import com.tiktel.ttelgo.esim.api.dto.ActivateBundleRequest;
import com.tiktel.ttelgo.esim.api.dto.ActivateBundleResponse;
import com.tiktel.ttelgo.esim.application.port.EsimGoProvisioningPort;
import com.tiktel.ttelgo.esim.application.port.EsimRepositoryPort;
import com.tiktel.ttelgo.esim.domain.Esim;
import com.tiktel.ttelgo.common.domain.enums.EsimStatus;
import com.tiktel.ttelgo.common.domain.enums.OrderStatus;
import com.tiktel.ttelgo.common.domain.enums.PaymentStatus;
import com.tiktel.ttelgo.integration.esimgo.dto.CreateOrderRequest;
import com.tiktel.ttelgo.integration.esimgo.dto.CreateOrderResponse;
import com.tiktel.ttelgo.integration.esimgo.dto.OrderDetailResponse;
import com.tiktel.ttelgo.integration.esimgo.dto.QrCodeResponse;
import com.tiktel.ttelgo.order.application.port.OrderRepositoryPort;
import com.tiktel.ttelgo.order.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * eSIM service for managing eSIM inventory
 */
@Slf4j
@Service
public class EsimService {
    
    private final EsimGoProvisioningPort esimGoProvisioningPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final EsimRepositoryPort esimRepositoryPort;
    
    @Autowired
    public EsimService(EsimGoProvisioningPort esimGoProvisioningPort,
                      OrderRepositoryPort orderRepositoryPort,
                      EsimRepositoryPort esimRepositoryPort) {
        this.esimGoProvisioningPort = esimGoProvisioningPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.esimRepositoryPort = esimRepositoryPort;
    }
    
    /**
     * Activate bundle after payment confirmation.
     * This is the main method called after Stripe payment is confirmed.
     */
    @Transactional
    public ActivateBundleResponse activateBundleAfterPayment(Long orderId) {
        log.info("Activating bundle after payment for orderId: {}", orderId);
        
        // Step 1: Get order from database
        Order order = orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        
        // Step 2: Create ActivateBundleRequest from order
        ActivateBundleRequest request = new ActivateBundleRequest();
        request.setType("transaction");
        request.setAssign(true);
        request.setUserId(order.getUserId());
        
        ActivateBundleRequest.OrderItem orderItem = new ActivateBundleRequest.OrderItem();
        orderItem.setType("bundle");
        orderItem.setItem(order.getBundleCode());
        orderItem.setQuantity(order.getQuantity());
        orderItem.setAllowReassign(false);
        request.setOrder(List.of(orderItem));
        
        // Step 3: Create order with eSIMGo API
        CreateOrderRequest createOrderRequest = mapToCreateOrderRequest(request);
        CreateOrderResponse esimGoResponse = esimGoProvisioningPort.createOrder(createOrderRequest);
        
        // Step 4: Update order and save eSIMs to database
        if (esimGoResponse != null && 
            (esimGoResponse.getStatus() != null && 
             (esimGoResponse.getStatus().equalsIgnoreCase("completed") || 
              esimGoResponse.getStatus().equalsIgnoreCase("success")))) {
            try {
                log.info("Saving order and eSIMs to database. Status: {}", esimGoResponse.getStatus());
                updateOrderAndSaveEsimsToDatabase(order, esimGoResponse);
                log.info("Successfully saved order and eSIMs to database");
            } catch (Exception e) {
                log.error("Error saving order and eSIMs to database: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to save order and eSIMs to database", e);
            }
        } else {
            log.warn("Skipping database save. Response status: {}", 
                esimGoResponse != null ? esimGoResponse.getStatus() : "null");
        }
        
        // Step 5: Return response
        return mapToActivateBundleResponse(esimGoResponse);
    }
    
    /**
     * Legacy method - kept for backward compatibility
     * Now creates order but doesn't activate eSIM (payment required first)
     */
    @Transactional
    public ActivateBundleResponse activateBundle(ActivateBundleRequest request) {
        // This method is deprecated - payment should be processed first
        // For now, we'll still allow it but log a warning
        log.warn("WARNING: activateBundle called without payment confirmation. " +
                "Consider using activateBundleAfterPayment instead.");
        
        // Step 1: Create order with eSIMGo API
        CreateOrderRequest createOrderRequest = mapToCreateOrderRequest(request);
        CreateOrderResponse esimGoResponse = esimGoProvisioningPort.createOrder(createOrderRequest);
        
        // Step 2: Save Order and Esim entities to database
        if (esimGoResponse != null && 
            (esimGoResponse.getStatus() != null && 
             (esimGoResponse.getStatus().equalsIgnoreCase("completed") || 
              esimGoResponse.getStatus().equalsIgnoreCase("success")))) {
            try {
                log.info("Saving order and eSIMs to database. Status: {}", esimGoResponse.getStatus());
                saveOrderAndEsimsToDatabase(request, esimGoResponse);
                log.info("Successfully saved order and eSIMs to database");
            } catch (Exception e) {
                log.error("Error saving order and eSIMs to database: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to save order and eSIMs to database", e);
            }
        }
        
        // Step 3: Return response
        return mapToActivateBundleResponse(esimGoResponse);
    }
    
    /**
     * Get QR code by matching ID, order ID, or order reference UUID.
     * This method is flexible and can handle:
     * - matchingId (e.g., "8LKJL-NPWQR-LDR1T-LIGOK")
     * - orderId (numeric, e.g., "27")
     * - orderReference UUID (e.g., "92688722-d069-4141-ba16-93fbafeea3e9")
     */
    public QrCodeResponse getQrCode(String identifier) {
        String matchingId = identifier;
        
        // Check if identifier is a numeric order ID
        try {
            Long orderId = Long.parseLong(identifier);
            log.info("Identifier is numeric orderId: {}", orderId);
            // Look up matchingId from database using orderId
            Optional<Esim> esim = esimRepositoryPort.findByOrderId(orderId);
            if (esim.isPresent() && esim.get().getMatchingId() != null) {
                matchingId = esim.get().getMatchingId();
                log.info("Found matchingId from orderId {}: {}", orderId, matchingId);
            } else {
                throw new RuntimeException("No eSIM found for orderId: " + orderId);
            }
        } catch (NumberFormatException e) {
            // Not a numeric ID, check if it's a UUID (orderReference)
            if (identifier.contains("-") && identifier.length() > 30) {
                log.info("Identifier looks like UUID (orderReference): {}", identifier);
                // Look up order by esimgo_order_id
                Optional<Order> order = orderRepositoryPort.findByOrderReference(identifier);
                if (order.isPresent()) {
                    Long orderId = order.get().getId();
                    log.info("Found order by orderReference {}: orderId={}", identifier, orderId);
                    // Look up matchingId from database using orderId
                    Optional<Esim> esim = esimRepositoryPort.findByOrderId(orderId);
                    if (esim.isPresent() && esim.get().getMatchingId() != null) {
                        matchingId = esim.get().getMatchingId();
                        log.info("Found matchingId from orderReference {}: {}", identifier, matchingId);
                    } else {
                        throw new RuntimeException("No eSIM found for orderReference: " + identifier);
                    }
                } else {
                    // Assume it's already a matchingId
                    log.info("Identifier not found as orderReference, using as matchingId: {}", identifier);
                }
            } else {
                // Assume it's already a matchingId
                log.info("Using identifier as matchingId: {}", identifier);
            }
        }
        
        return esimGoProvisioningPort.getQrCode(matchingId);
    }
    
    /**
     * Get eSIM provider order details by provider order ID
     */
    public ActivateBundleResponse getOrderDetails(String orderId) {
        OrderDetailResponse response = esimGoProvisioningPort.getOrderDetails(orderId);
        return mapOrderDetailToResponse(response);
    }
    
    /**
     * Update existing order and save eSIMs to database after successful eSIMGo activation
     */
    private void updateOrderAndSaveEsimsToDatabase(Order order, CreateOrderResponse esimGoResponse) {
        log.info("=== Starting database save process ===");
        log.info("OrderId: {}", order.getId());
        log.info("OrderReference: {}", esimGoResponse.getOrderReference());
        log.info("Total: {}", esimGoResponse.getTotal());
        log.info("Currency: {}", esimGoResponse.getCurrency());
        
        // Update order with eSIMGo response data
        String orderReference = esimGoResponse.getOrderReference();
        if (orderReference == null || orderReference.trim().isEmpty()) {
            orderReference = UUID.randomUUID().toString();
            log.warn("Warning: orderReference was null, generated new UUID: {}", orderReference);
        }
        
        // Get price per unit
        BigDecimal unitPrice = BigDecimal.ZERO;
        if (esimGoResponse.getOrder() != null && !esimGoResponse.getOrder().isEmpty()) {
            Double pricePerUnit = esimGoResponse.getOrder().get(0).getPricePerUnit();
            if (pricePerUnit != null) {
                unitPrice = BigDecimal.valueOf(pricePerUnit);
            }
        }
        
        // Update order
        LocalDateTime now = LocalDateTime.now();
        order.setEsimgoOrderId(esimGoResponse.getOrderReference());
        order.setUnitPrice(unitPrice);
        order.setTotalAmount(esimGoResponse.getTotal() != null 
            ? BigDecimal.valueOf(esimGoResponse.getTotal()) 
            : BigDecimal.ZERO);
        order.setCurrency(esimGoResponse.getCurrency() != null ? esimGoResponse.getCurrency() : "USD");
        order.setStatus(OrderStatus.COMPLETED);
        order.setPaymentStatus(PaymentStatus.SUCCEEDED);
        // Set timestamps
        if (order.getPaidAt() == null) {
            order.setPaidAt(now);
        }
        order.setProvisionedAt(now);
        order.setCompletedAt(now);
        
        Order savedOrder = orderRepositoryPort.save(order);
        log.info("Order updated: id={}", savedOrder.getId());
        
        // Save eSIMs
        saveEsimsToDatabase(savedOrder, esimGoResponse);
    }
    
    /**
     * Save Order and Esim entities to database after successful eSIMGo activation
     */
    private void saveOrderAndEsimsToDatabase(ActivateBundleRequest request, CreateOrderResponse esimGoResponse) {
        log.info("=== Starting database save process ===");
        log.info("OrderReference: {}", esimGoResponse.getOrderReference());
        log.info("Total: {}", esimGoResponse.getTotal());
        log.info("Currency: {}", esimGoResponse.getCurrency());
        log.info("UserId: {}", request.getUserId());
        
        // Extract bundle information from request
        String bundleId = null;
        String bundleName = null;
        if (request.getOrder() != null && !request.getOrder().isEmpty()) {
            bundleId = request.getOrder().get(0).getItem();
            bundleName = request.getOrder().get(0).getItem(); // Bundle name is the same as bundle ID
            log.info("BundleId: {}", bundleId);
            log.info("Quantity: {}", request.getOrder().get(0).getQuantity());
        }
        
        // Validate orderReference is not null (required field)
        String orderReference = esimGoResponse.getOrderReference();
        if (orderReference == null || orderReference.trim().isEmpty()) {
            orderReference = UUID.randomUUID().toString();
            log.warn("Warning: orderReference was null, generated new UUID: {}", orderReference);
        }
        
        // Get price per unit
        BigDecimal unitPrice = BigDecimal.ZERO;
        if (esimGoResponse.getOrder() != null && !esimGoResponse.getOrder().isEmpty()) {
            Double pricePerUnit = esimGoResponse.getOrder().get(0).getPricePerUnit();
            if (pricePerUnit != null) {
                unitPrice = BigDecimal.valueOf(pricePerUnit);
            }
        }
        
        // Extract first eSIM info
        String matchingId = null;
        String iccid = null;
        String smdpAddress = null;
        if (esimGoResponse.getOrder() != null && !esimGoResponse.getOrder().isEmpty()) {
            CreateOrderResponse.OrderDetail orderDetail = esimGoResponse.getOrder().get(0);
            if (orderDetail.getEsims() != null && !orderDetail.getEsims().isEmpty()) {
                CreateOrderResponse.EsimInfo firstEsim = orderDetail.getEsims().get(0);
                matchingId = firstEsim.getMatchingId();
                iccid = firstEsim.getIccid();
                smdpAddress = firstEsim.getSmdpAddress();
            }
        }
        
        // Create and save Order entity
        Order order = Order.builder()
                .orderNumber(orderReference)
                .userId(request.getUserId())
                .bundleCode(bundleId)
                .bundleName(bundleName)
                .quantity(request.getOrder() != null && !request.getOrder().isEmpty() 
                    ? request.getOrder().get(0).getQuantity() 
                    : 1)
                .unitPrice(unitPrice)
                .totalAmount(esimGoResponse.getTotal() != null 
                    ? BigDecimal.valueOf(esimGoResponse.getTotal()) 
                    : BigDecimal.ZERO)
                .currency(esimGoResponse.getCurrency() != null ? esimGoResponse.getCurrency() : "USD")
                .status(OrderStatus.COMPLETED) // Order is completed after successful eSIMGo activation
                .paymentStatus(PaymentStatus.SUCCEEDED) // Payment is successful if eSIMGo accepted the order
                .esimgoOrderId(esimGoResponse.getOrderReference())
                .build();
        
        Order savedOrder = orderRepositoryPort.save(order);
        log.info("Order saved: id={}", savedOrder.getId());
        
        // Save eSIMs
        saveEsimsToDatabase(savedOrder, esimGoResponse);
    }
    
    /**
     * Save eSIM entities to database
     */
    private void saveEsimsToDatabase(Order savedOrder, CreateOrderResponse esimGoResponse) {
        if (esimGoResponse.getOrder() == null || esimGoResponse.getOrder().isEmpty()) {
            log.warn("No order details in eSIMGo response");
            return;
        }
        
        for (CreateOrderResponse.OrderDetail orderDetail : esimGoResponse.getOrder()) {
            if (orderDetail.getEsims() != null && !orderDetail.getEsims().isEmpty()) {
                for (CreateOrderResponse.EsimInfo esimInfo : orderDetail.getEsims()) {
                    Esim esim = Esim.builder()
                            .orderId(savedOrder.getId())
                            .userId(savedOrder.getUserId())
                            .bundleCode(savedOrder.getBundleCode())
                            .bundleName(savedOrder.getBundleName())
                            .matchingId(esimInfo.getMatchingId())
                            .iccid(esimInfo.getIccid())
                            .smdpAddress(esimInfo.getSmdpAddress())
                            .status(EsimStatus.CREATED)
                            .build();
                    
                    esimRepositoryPort.save(esim);
                    log.info("eSIM saved: iccid={}, matchingId={}", esimInfo.getIccid(), esimInfo.getMatchingId());
                }
            }
        }
    }
    
    /**
     * Map ActivateBundleRequest to CreateOrderRequest (eSIMGo format)
     */
    private CreateOrderRequest mapToCreateOrderRequest(ActivateBundleRequest request) {
        CreateOrderRequest createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setType(request.getType());
        createOrderRequest.setAssign(request.getAssign());
        
        if (request.getOrder() != null && !request.getOrder().isEmpty()) {
            List<CreateOrderRequest.OrderItem> orderItems = request.getOrder().stream()
                    .map(item -> {
                        CreateOrderRequest.OrderItem orderItem = new CreateOrderRequest.OrderItem();
                        orderItem.setType(item.getType());
                        orderItem.setItem(item.getItem());
                        orderItem.setQuantity(item.getQuantity());
                        orderItem.setAllowReassign(item.getAllowReassign());
                        return orderItem;
                    })
                    .collect(Collectors.toList());
            createOrderRequest.setOrder(orderItems);
        }
        
        return createOrderRequest;
    }
    
    /**
     * Map CreateOrderResponse to ActivateBundleResponse
     */
    private ActivateBundleResponse mapToActivateBundleResponse(CreateOrderResponse esimGoResponse) {
        if (esimGoResponse == null) {
            return null;
        }
        
        ActivateBundleResponse response = new ActivateBundleResponse();
        response.setTotal(esimGoResponse.getTotal());
        response.setCurrency(esimGoResponse.getCurrency());
        response.setStatus(esimGoResponse.getStatus());
        response.setStatusMessage(esimGoResponse.getStatusMessage());
        response.setOrderReference(esimGoResponse.getOrderReference());
        response.setCreatedDate(esimGoResponse.getCreatedDate());
        response.setAssigned(esimGoResponse.getAssigned());
        
        if (esimGoResponse.getOrder() != null) {
            List<ActivateBundleResponse.OrderDetail> orderDetails = esimGoResponse.getOrder().stream()
                    .map(detail -> {
                        ActivateBundleResponse.OrderDetail orderDetail = new ActivateBundleResponse.OrderDetail();
                        orderDetail.setType(detail.getType());
                        orderDetail.setItem(detail.getItem());
                        orderDetail.setQuantity(detail.getQuantity());
                        orderDetail.setSubTotal(detail.getSubTotal());
                        orderDetail.setPricePerUnit(detail.getPricePerUnit());
                        orderDetail.setAllowReassign(detail.getAllowReassign());
                        orderDetail.setIccids(detail.getIccids());
                        
                        if (detail.getEsims() != null) {
                            List<ActivateBundleResponse.EsimInfo> esimInfos = detail.getEsims().stream()
                                    .map(esim -> {
                                        ActivateBundleResponse.EsimInfo esimInfo = new ActivateBundleResponse.EsimInfo();
                                        esimInfo.setIccid(esim.getIccid());
                                        esimInfo.setMatchingId(esim.getMatchingId());
                                        esimInfo.setSmdpAddress(esim.getSmdpAddress());
                                        return esimInfo;
                                    })
                                    .collect(Collectors.toList());
                            orderDetail.setEsims(esimInfos);
                        }
                        
                        return orderDetail;
                    })
                    .collect(Collectors.toList());
            response.setOrder(orderDetails);
        }
        
        return response;
    }
    
    /**
     * Map OrderDetailResponse to ActivateBundleResponse
     */
    private ActivateBundleResponse mapOrderDetailToResponse(OrderDetailResponse response) {
        if (response == null) {
            return null;
        }
        
        ActivateBundleResponse activateResponse = new ActivateBundleResponse();
        activateResponse.setTotal(response.getTotal());
        activateResponse.setCurrency(response.getCurrency());
        activateResponse.setStatus(response.getStatus());
        activateResponse.setStatusMessage(response.getStatusMessage());
        activateResponse.setOrderReference(response.getOrderReference());
        activateResponse.setCreatedDate(response.getCreatedDate());
        activateResponse.setAssigned(response.getAssigned());
        
        if (response.getOrder() != null) {
            List<ActivateBundleResponse.OrderDetail> orderDetails = response.getOrder().stream()
                    .map(detail -> {
                        ActivateBundleResponse.OrderDetail orderDetail = new ActivateBundleResponse.OrderDetail();
                        orderDetail.setType(detail.getType());
                        orderDetail.setItem(detail.getItem());
                        orderDetail.setQuantity(detail.getQuantity());
                        orderDetail.setSubTotal(detail.getSubTotal());
                        orderDetail.setPricePerUnit(detail.getPricePerUnit());
                        orderDetail.setAllowReassign(detail.getAllowReassign());
                        orderDetail.setIccids(detail.getIccids());
                        
                        if (detail.getEsims() != null) {
                            List<ActivateBundleResponse.EsimInfo> esimInfos = detail.getEsims().stream()
                                    .map(esim -> {
                                        ActivateBundleResponse.EsimInfo esimInfo = new ActivateBundleResponse.EsimInfo();
                                        esimInfo.setIccid(esim.getIccid());
                                        esimInfo.setMatchingId(esim.getMatchingId());
                                        esimInfo.setSmdpAddress(esim.getSmdpAddress());
                                        return esimInfo;
                                    })
                                    .collect(Collectors.toList());
                            orderDetail.setEsims(esimInfos);
                        }
                        
                        return orderDetail;
                    })
                    .collect(Collectors.toList());
            activateResponse.setOrder(orderDetails);
        }
        
        return activateResponse;
    }
    
    /**
     * Mark expired eSIMs based on validUntil date
     */
    @Transactional
    public int markExpiredEsims() {
        LocalDateTime now = LocalDateTime.now();
        List<Esim> expiredEsims = esimRepositoryPort.findByStatus(EsimStatus.ACTIVE).stream()
                .filter(esim -> esim.getValidUntil() != null && esim.getValidUntil().isBefore(now))
                .collect(Collectors.toList());
        
        for (Esim esim : expiredEsims) {
            esim.setStatus(EsimStatus.EXPIRED);
            esim.setExpiredAt(now);
            esimRepositoryPort.save(esim);
        }
        
        return expiredEsims.size();
    }
}
