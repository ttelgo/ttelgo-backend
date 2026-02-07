package com.tiktel.ttelgo.order.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.dto.PageRequest;
import com.tiktel.ttelgo.common.dto.PageResponse;
import com.tiktel.ttelgo.common.idempotency.IdempotencyService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * B2C Order API for customers
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Orders (B2C)", description = "Customer order management")
public class OrderController {
    
    private final OrderService orderService;
    private final IdempotencyService idempotencyService;
    private final OrderApiMapper orderApiMapper;
    
    public OrderController(OrderService orderService,
                          IdempotencyService idempotencyService,
                          OrderApiMapper orderApiMapper) {
        this.orderService = orderService;
        this.idempotencyService = idempotencyService;
        this.orderApiMapper = orderApiMapper;
    }
    
    @Operation(summary = "Create order", description = "Create a new eSIM order (requires Idempotency-Key header)")
    @PostMapping
    public ApiResponse<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        log.info("Creating order: bundleCode={}, quantity={}", 
                request.getBundleCode(), request.getQuantity());
        
        Long userId = extractUserId(authentication);
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        
        // Check idempotency
        if (idempotencyKey != null) {
            var existing = idempotencyService.checkIdempotency(
                    idempotencyKey, userId, null, "/api/v1/orders", "POST", request);
            
            if (existing.isPresent()) {
                log.info("Idempotent request, returning existing response");
                OrderResponse response = orderApiMapper.parseOrderResponse(existing.get().responseBody());
                return ApiResponse.success(response);
            }
        }
        
        // Create order
        Order order = orderService.createB2COrder(
                userId,
                request.getCustomerEmail(),
                request.getBundleCode(),
                request.getQuantity(),
                ipAddress,
                userAgent
        );
        
        OrderResponse response = orderApiMapper.toResponse(order);
        
        // Store idempotency record
        if (idempotencyKey != null) {
            String responseBody = orderApiMapper.serializeOrderResponse(response);
            idempotencyService.storeIdempotencyRecord(
                    idempotencyKey, userId, null, "/api/v1/orders", "POST",
                    request, 200, responseBody, "ORDER", order.getId());
        }
        
        return ApiResponse.success(response);
    }

    @Operation(summary = "Get my orders", description = "Get all orders for the current user")
    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> getMyOrders(
            @ModelAttribute PageRequest pageRequest,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        log.info("Getting orders for user: {}", userId);

        Page<Order> orders = orderService.getUserOrders(userId, pageRequest.toPageable("createdAt"));
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
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        log.info("Getting order: orderId={}, userId={}", orderId, userId);

        Order order = orderService.getOrderById(orderId);

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        return ApiResponse.success(orderApiMapper.toResponse(order));
    }

    private Long extractUserId(Authentication authentication) {
        return 1L;
    }
}
