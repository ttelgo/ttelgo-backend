package com.tiktel.ttelgo.esim.application;

import com.tiktel.ttelgo.esim.api.dto.ActivateBundleRequest;
import com.tiktel.ttelgo.esim.api.dto.ActivateBundleResponse;
import com.tiktel.ttelgo.esim.application.port.EsimRepositoryPort;
import com.tiktel.ttelgo.esim.domain.Esim;
import com.tiktel.ttelgo.esim.domain.EsimStatus;
import com.tiktel.ttelgo.integration.esimgo.EsimGoClient;
import com.tiktel.ttelgo.integration.esimgo.dto.CreateOrderRequest;
import com.tiktel.ttelgo.integration.esimgo.dto.CreateOrderResponse;
import com.tiktel.ttelgo.integration.esimgo.dto.OrderDetailResponse;
import com.tiktel.ttelgo.integration.esimgo.dto.QrCodeResponse;
import com.tiktel.ttelgo.order.application.port.OrderRepositoryPort;
import com.tiktel.ttelgo.order.domain.Order;
import com.tiktel.ttelgo.order.domain.OrderStatus;
import com.tiktel.ttelgo.order.domain.PaymentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EsimService {
    
    private final EsimGoClient esimGoClient;
    private final OrderRepositoryPort orderRepositoryPort;
    private final EsimRepositoryPort esimRepositoryPort;
    
    @Autowired
    public EsimService(EsimGoClient esimGoClient, 
                      OrderRepositoryPort orderRepositoryPort,
                      EsimRepositoryPort esimRepositoryPort) {
        this.esimGoClient = esimGoClient;
        this.orderRepositoryPort = orderRepositoryPort;
        this.esimRepositoryPort = esimRepositoryPort;
    }
    
    /**
     * Activate bundle after payment confirmation
     * This is called after Stripe payment is confirmed
     * @param orderId The order ID that was created during payment intent creation
     */
    @Transactional
    public ActivateBundleResponse activateBundleAfterPayment(Long orderId) {
        // Step 1: Get the order
        Order order = orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        // Step 2: Verify payment status
        if (order.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new RuntimeException("Payment not confirmed for order: " + orderId);
        }
        
        // Step 3: Create ActivateBundleRequest from order
        ActivateBundleRequest request = new ActivateBundleRequest();
        request.setType("transaction");
        request.setAssign(true);
        request.setUserId(order.getUserId());
        
        ActivateBundleRequest.OrderItem orderItem = new ActivateBundleRequest.OrderItem();
        orderItem.setType("bundle");
        orderItem.setItem(order.getBundleId());
        orderItem.setQuantity(order.getQuantity());
        orderItem.setAllowReassign(false);
        request.setOrder(java.util.List.of(orderItem));
        
        // Step 4: Create order with eSIMGo API
        CreateOrderRequest createOrderRequest = mapToCreateOrderRequest(request);
        CreateOrderResponse esimGoResponse = esimGoClient.createOrder(createOrderRequest);
        
        // Step 5: Update order and save eSIMs to database
        if (esimGoResponse != null && 
            (esimGoResponse.getStatus() != null && 
             (esimGoResponse.getStatus().equalsIgnoreCase("completed") || 
              esimGoResponse.getStatus().equalsIgnoreCase("success")))) {
            try {
                System.out.println("Saving order and eSIMs to database. Status: " + esimGoResponse.getStatus());
                updateOrderAndSaveEsimsToDatabase(order, esimGoResponse);
                System.out.println("Successfully saved order and eSIMs to database");
            } catch (Exception e) {
                System.err.println("Error saving order and eSIMs to database: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to save order and eSIMs to database", e);
            }
        } else {
            System.out.println("Skipping database save. Response status: " + 
                (esimGoResponse != null ? esimGoResponse.getStatus() : "null"));
        }
        
        // Step 6: Return response
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
        System.out.println("WARNING: activateBundle called without payment confirmation. " +
                "Consider using activateBundleAfterPayment instead.");
        
        // Step 1: Create order with eSIMGo API
        CreateOrderRequest createOrderRequest = mapToCreateOrderRequest(request);
        CreateOrderResponse esimGoResponse = esimGoClient.createOrder(createOrderRequest);
        
        // Step 2: Save Order and Esim entities to database
        if (esimGoResponse != null && 
            (esimGoResponse.getStatus() != null && 
             (esimGoResponse.getStatus().equalsIgnoreCase("completed") || 
              esimGoResponse.getStatus().equalsIgnoreCase("success")))) {
            try {
                System.out.println("Saving order and eSIMs to database. Status: " + esimGoResponse.getStatus());
                saveOrderAndEsimsToDatabase(request, esimGoResponse);
                System.out.println("Successfully saved order and eSIMs to database");
            } catch (Exception e) {
                System.err.println("Error saving order and eSIMs to database: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to save order and eSIMs to database", e);
            }
        }
        
        // Step 3: Return response
        return mapToActivateBundleResponse(esimGoResponse);
    }
    
    /**
     * Save Order and Esim entities to database after successful eSIMGo activation
     */
    private void saveOrderAndEsimsToDatabase(ActivateBundleRequest request, CreateOrderResponse esimGoResponse) {
        System.out.println("=== Starting database save process ===");
        System.out.println("OrderReference: " + esimGoResponse.getOrderReference());
        System.out.println("Total: " + esimGoResponse.getTotal());
        System.out.println("Currency: " + esimGoResponse.getCurrency());
        System.out.println("UserId: " + request.getUserId());
        
        // Extract bundle information from request
        String bundleId = null;
        String bundleName = null;
        if (request.getOrder() != null && !request.getOrder().isEmpty()) {
            bundleId = request.getOrder().get(0).getItem();
            bundleName = request.getOrder().get(0).getItem(); // Bundle name is the same as bundle ID
            System.out.println("BundleId: " + bundleId);
            System.out.println("Quantity: " + request.getOrder().get(0).getQuantity());
        }
        
        // Validate orderReference is not null (required field)
        String orderReference = esimGoResponse.getOrderReference();
        if (orderReference == null || orderReference.trim().isEmpty()) {
            orderReference = UUID.randomUUID().toString();
            System.out.println("Warning: orderReference was null, generated new UUID: " + orderReference);
        }
        
        // Get price per unit
        BigDecimal unitPrice = BigDecimal.ZERO;
        if (esimGoResponse.getOrder() != null && !esimGoResponse.getOrder().isEmpty()) {
            Double pricePerUnit = esimGoResponse.getOrder().get(0).getPricePerUnit();
            if (pricePerUnit != null) {
                unitPrice = BigDecimal.valueOf(pricePerUnit);
            }
        }
        
        // Create and save Order entity
        Order order = Order.builder()
                .orderReference(orderReference)
                .userId(request.getUserId())
                .bundleId(bundleId)
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
                .paymentStatus(PaymentStatus.SUCCESS) // Payment is successful if eSIMGo accepted the order
                .esimgoOrderId(esimGoResponse.getOrderReference())
                .build();
        
        // Set matchingId and iccid from first eSIM if available
        if (esimGoResponse.getOrder() != null && !esimGoResponse.getOrder().isEmpty()) {
            CreateOrderResponse.OrderDetail firstOrderDetail = esimGoResponse.getOrder().get(0);
            if (firstOrderDetail.getEsims() != null && !firstOrderDetail.getEsims().isEmpty()) {
                CreateOrderResponse.EsimInfo firstEsim = firstOrderDetail.getEsims().get(0);
                order.setMatchingId(firstEsim.getMatchingId());
                order.setIccid(firstEsim.getIccid());
                order.setSmdpAddress(firstEsim.getSmdpAddress());
                System.out.println("MatchingId: " + firstEsim.getMatchingId());
                System.out.println("ICCID: " + firstEsim.getIccid());
            }
        }
        
        // Save Order to database
        System.out.println("Saving Order to database...");
        Order savedOrder = orderRepositoryPort.save(order);
        System.out.println("Order saved with ID: " + savedOrder.getId());
        
        // Create and save Esim entities for each eSIM in the response
        int esimCount = 0;
        if (esimGoResponse.getOrder() != null) {
            for (CreateOrderResponse.OrderDetail orderDetail : esimGoResponse.getOrder()) {
                if (orderDetail.getEsims() != null) {
                    for (CreateOrderResponse.EsimInfo esimInfo : orderDetail.getEsims()) {
                        esimCount++;
                        System.out.println("Creating eSIM #" + esimCount + " - MatchingId: " + esimInfo.getMatchingId());
                        
                        Esim esim = Esim.builder()
                                .esimUuid(UUID.randomUUID().toString()) // Generate UUID for QR code endpoint
                                .orderId(savedOrder.getId())
                                .userId(request.getUserId())
                                .bundleId(bundleId)
                                .bundleName(bundleName)
                                .matchingId(esimInfo.getMatchingId())
                                .iccid(esimInfo.getIccid())
                                .smdpAddress(esimInfo.getSmdpAddress())
                                .status(EsimStatus.PROVISIONED) // eSIM is provisioned after successful activation
                                .esimgoOrderId(esimGoResponse.getOrderReference())
                                .build();
                        
                        // Save Esim to database
                        System.out.println("Saving eSIM to database...");
                        Esim savedEsim = esimRepositoryPort.save(esim);
                        System.out.println("eSIM saved with ID: " + savedEsim.getId());
                    }
                }
            }
        }
        
        System.out.println("=== Database save process completed. Saved " + esimCount + " eSIM(s) ===");
    }
    
    /**
     * Update existing order and save eSIMs to database after eSIMGo activation
     * Used when order was already created during payment intent creation
     */
    private void updateOrderAndSaveEsimsToDatabase(Order order, CreateOrderResponse esimGoResponse) {
        System.out.println("=== Starting database update process ===");
        System.out.println("OrderReference: " + esimGoResponse.getOrderReference());
        System.out.println("Total: " + esimGoResponse.getTotal());
        System.out.println("Currency: " + esimGoResponse.getCurrency());
        System.out.println("Order ID: " + order.getId());
        
        // Update order with eSIMGo response data
        String orderReference = esimGoResponse.getOrderReference();
        if (orderReference != null && !orderReference.trim().isEmpty()) {
            order.setOrderReference(orderReference);
        }
        
        // Get price per unit
        BigDecimal unitPrice = BigDecimal.ZERO;
        if (esimGoResponse.getOrder() != null && !esimGoResponse.getOrder().isEmpty()) {
            Double pricePerUnit = esimGoResponse.getOrder().get(0).getPricePerUnit();
            if (pricePerUnit != null) {
                unitPrice = BigDecimal.valueOf(pricePerUnit);
                order.setUnitPrice(unitPrice);
            }
        }
        
        // Update order status
        order.setStatus(OrderStatus.COMPLETED);
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setEsimgoOrderId(esimGoResponse.getOrderReference());
        
        // Set matchingId and iccid from first eSIM if available
        if (esimGoResponse.getOrder() != null && !esimGoResponse.getOrder().isEmpty()) {
            CreateOrderResponse.OrderDetail firstOrderDetail = esimGoResponse.getOrder().get(0);
            if (firstOrderDetail.getEsims() != null && !firstOrderDetail.getEsims().isEmpty()) {
                CreateOrderResponse.EsimInfo firstEsim = firstOrderDetail.getEsims().get(0);
                order.setMatchingId(firstEsim.getMatchingId());
                order.setIccid(firstEsim.getIccid());
                order.setSmdpAddress(firstEsim.getSmdpAddress());
                System.out.println("MatchingId: " + firstEsim.getMatchingId());
                System.out.println("ICCID: " + firstEsim.getIccid());
            }
        }
        
        // Update Order in database
        System.out.println("Updating Order in database...");
        Order updatedOrder = orderRepositoryPort.save(order);
        System.out.println("Order updated with ID: " + updatedOrder.getId());
        
        // Create and save Esim entities for each eSIM in the response
        int esimCount = 0;
        if (esimGoResponse.getOrder() != null) {
            for (CreateOrderResponse.OrderDetail orderDetail : esimGoResponse.getOrder()) {
                if (orderDetail.getEsims() != null) {
                    for (CreateOrderResponse.EsimInfo esimInfo : orderDetail.getEsims()) {
                        esimCount++;
                        System.out.println("Creating eSIM #" + esimCount + " - MatchingId: " + esimInfo.getMatchingId());
                        
                        Esim esim = Esim.builder()
                                .esimUuid(UUID.randomUUID().toString())
                                .orderId(updatedOrder.getId())
                                .userId(order.getUserId())
                                .bundleId(order.getBundleId())
                                .bundleName(order.getBundleName())
                                .matchingId(esimInfo.getMatchingId())
                                .iccid(esimInfo.getIccid())
                                .smdpAddress(esimInfo.getSmdpAddress())
                                .status(EsimStatus.PROVISIONED)
                                .esimgoOrderId(esimGoResponse.getOrderReference())
                                .build();
                        
                        // Save Esim to database
                        System.out.println("Saving eSIM to database...");
                        Esim savedEsim = esimRepositoryPort.save(esim);
                        System.out.println("eSIM saved with ID: " + savedEsim.getId());
                    }
                }
            }
        }
        
        System.out.println("=== Database update process completed. Saved " + esimCount + " eSIM(s) ===");
    }
    
    public QrCodeResponse getQrCode(String matchingId) {
        return esimGoClient.getQrCode(matchingId);
    }
    
    public ActivateBundleResponse getOrderDetails(String orderId) {
        OrderDetailResponse response = esimGoClient.getOrderDetails(orderId);
        return mapOrderDetailToResponse(response);
    }
    
    private CreateOrderRequest mapToCreateOrderRequest(ActivateBundleRequest request) {
        CreateOrderRequest createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setType(request.getType());
        createOrderRequest.setAssign(request.getAssign());
        
        if (request.getOrder() != null) {
            createOrderRequest.setOrder(request.getOrder().stream()
                    .map(item -> {
                        CreateOrderRequest.OrderItem orderItem = new CreateOrderRequest.OrderItem();
                        orderItem.setType(item.getType());
                        orderItem.setItem(item.getItem());
                        orderItem.setQuantity(item.getQuantity());
                        orderItem.setAllowReassign(item.getAllowReassign());
                        return orderItem;
                    })
                    .collect(Collectors.toList()));
        }
        
        return createOrderRequest;
    }
    
    private ActivateBundleResponse mapToActivateBundleResponse(CreateOrderResponse response) {
        ActivateBundleResponse result = new ActivateBundleResponse();
        result.setTotal(response.getTotal());
        result.setCurrency(response.getCurrency());
        result.setStatus(response.getStatus());
        result.setStatusMessage(response.getStatusMessage());
        result.setOrderReference(response.getOrderReference());
        result.setCreatedDate(response.getCreatedDate());
        result.setAssigned(response.getAssigned());
        
        if (response.getOrder() != null) {
            result.setOrder(response.getOrder().stream()
                    .map(orderDetail -> {
                        ActivateBundleResponse.OrderDetail detail = new ActivateBundleResponse.OrderDetail();
                        detail.setType(orderDetail.getType());
                        detail.setItem(orderDetail.getItem());
                        detail.setIccids(orderDetail.getIccids());
                        detail.setQuantity(orderDetail.getQuantity());
                        detail.setSubTotal(orderDetail.getSubTotal());
                        detail.setPricePerUnit(orderDetail.getPricePerUnit());
                        detail.setAllowReassign(orderDetail.getAllowReassign());
                        
                        if (orderDetail.getEsims() != null) {
                            detail.setEsims(orderDetail.getEsims().stream()
                                    .map(esim -> {
                                        ActivateBundleResponse.EsimInfo esimInfo = new ActivateBundleResponse.EsimInfo();
                                        esimInfo.setIccid(esim.getIccid());
                                        esimInfo.setMatchingId(esim.getMatchingId());
                                        esimInfo.setSmdpAddress(esim.getSmdpAddress());
                                        return esimInfo;
                                    })
                                    .collect(Collectors.toList()));
                        }
                        
                        return detail;
                    })
                    .collect(Collectors.toList()));
        }
        
        return result;
    }
    
    private ActivateBundleResponse mapOrderDetailToResponse(OrderDetailResponse response) {
        ActivateBundleResponse result = new ActivateBundleResponse();
        result.setTotal(response.getTotal());
        result.setCurrency(response.getCurrency());
        result.setStatus(response.getStatus());
        result.setStatusMessage(response.getStatusMessage());
        result.setOrderReference(response.getOrderReference());
        result.setCreatedDate(response.getCreatedDate());
        result.setAssigned(response.getAssigned());
        
        if (response.getOrder() != null) {
            result.setOrder(response.getOrder().stream()
                    .map(orderDetail -> {
                        ActivateBundleResponse.OrderDetail detail = new ActivateBundleResponse.OrderDetail();
                        detail.setType(orderDetail.getType());
                        detail.setItem(orderDetail.getItem());
                        detail.setIccids(orderDetail.getIccids());
                        detail.setQuantity(orderDetail.getQuantity());
                        detail.setSubTotal(orderDetail.getSubTotal());
                        detail.setPricePerUnit(orderDetail.getPricePerUnit());
                        detail.setAllowReassign(orderDetail.getAllowReassign());
                        
                        if (orderDetail.getEsims() != null) {
                            detail.setEsims(orderDetail.getEsims().stream()
                                    .map(esim -> {
                                        ActivateBundleResponse.EsimInfo esimInfo = new ActivateBundleResponse.EsimInfo();
                                        esimInfo.setIccid(esim.getIccid());
                                        esimInfo.setMatchingId(esim.getMatchingId());
                                        esimInfo.setSmdpAddress(esim.getSmdpAddress());
                                        return esimInfo;
                                    })
                                    .collect(Collectors.toList()));
                        }
                        
                        return detail;
                    })
                    .collect(Collectors.toList()));
        }
        
        return result;
    }
}
