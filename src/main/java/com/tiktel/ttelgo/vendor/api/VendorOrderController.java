package com.tiktel.ttelgo.vendor.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.dto.PageRequest;
import com.tiktel.ttelgo.common.dto.PageResponse;
import com.tiktel.ttelgo.order.api.dto.CreateOrderRequest;
import com.tiktel.ttelgo.order.api.dto.OrderResponse;
import com.tiktel.ttelgo.order.api.mapper.OrderApiMapper;
import com.tiktel.ttelgo.order.application.OrderService;
import com.tiktel.ttelgo.order.domain.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * B2B Vendor Order API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/vendor/orders")
@SecurityRequirement(name = "API Key Authentication")
@Tag(name = "Vendor Orders (B2B)", description = "Vendor order management")
public class VendorOrderController {
    
    private final OrderService orderService;
    private final OrderApiMapper orderApiMapper;
    
    public VendorOrderController(OrderService orderService, OrderApiMapper orderApiMapper) {
        this.orderService = orderService;
        this.orderApiMapper = orderApiMapper;
    }
    
    @Operation(summary = "Create vendor order", description = "Create a new order for vendor")
    @PostMapping
    public ApiResponse<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader("X-API-Key") String apiKey,
            HttpServletRequest httpRequest) {
        
        log.info("Creating vendor order: bundleCode={}, quantity={}",
                request.getBundleCode(), request.getQuantity());
        
        Long vendorId = extractVendorId(apiKey);
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        
        Order order = orderService.createB2BOrder(
                vendorId,
                request.getBundleCode(),
                request.getQuantity(),
                ipAddress,
                userAgent
        );
        
        return ApiResponse.success(orderApiMapper.toResponse(order));
    }
    
    @Operation(summary = "Get vendor orders", description = "Get all orders for vendor")
    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> getOrders(
            @ModelAttribute PageRequest pageRequest,
            @RequestHeader("X-API-Key") String apiKey) {
        
        Long vendorId = extractVendorId(apiKey);
        log.info("Getting orders for vendor: {}", vendorId);
        
        Page<Order> orders = orderService.getVendorOrders(vendorId, pageRequest.toPageable("createdAt"));
        PageResponse<OrderResponse> response = PageResponse.of(
                orders.getContent().stream().map(orderApiMapper::toResponse).toList(),
                orders
        );
        
        return ApiResponse.success(response);
    }
    
    @Operation(summary = "Get order details", description = "Get details of a specific order")
    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-API-Key") String apiKey) {
        
        Long vendorId = extractVendorId(apiKey);
        Order order = orderService.getOrderById(orderId);
        
        // Verify order belongs to vendor
        if (!order.getVendorId().equals(vendorId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        return ApiResponse.success(orderApiMapper.toResponse(order));
    }
    
    private Long extractVendorId(String apiKey) {
        // TODO: Validate API key and extract vendor ID
        return 1L; // Placeholder
    }
}

