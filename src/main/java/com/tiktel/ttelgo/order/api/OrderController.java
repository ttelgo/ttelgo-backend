package com.tiktel.ttelgo.order.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
<<<<<<< HEAD
import com.tiktel.ttelgo.common.dto.PageRequest;
import com.tiktel.ttelgo.common.dto.PageResponse;
import com.tiktel.ttelgo.common.idempotency.IdempotencyService;
import com.tiktel.ttelgo.order.api.dto.CreateOrderRequest;
=======
import com.tiktel.ttelgo.common.dto.PaginationMeta;
>>>>>>> 517cfdbabcad5678433bdd3ff85dacd99c0cfaeb
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

<<<<<<< HEAD
/**
 * B2C Order API for customers
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Orders (B2C)", description = "Customer order management")
=======
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
>>>>>>> 517cfdbabcad5678433bdd3ff85dacd99c0cfaeb
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
    
<<<<<<< HEAD
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
        
        // Verify order belongs to user
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        return ApiResponse.success(orderApiMapper.toResponse(order));
    }
    
    private Long extractUserId(Authentication authentication) {
        // TODO: Extract user ID from JWT token
        return 1L; // Placeholder
=======
    /**
     * RESTful query-style lookups.
     * Examples:
     * - GET /api/v1/orders?reference=abc-uuid
     * - GET /api/v1/orders?userId=123
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> findOrders(
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer size,
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort
    ) {
        if (reference != null && !reference.trim().isEmpty()) {
            OrderResponse response = orderService.getOrderByReference(reference.trim());
            return ResponseEntity.ok(ApiResponse.success(response));
        }
        if (userId != null) {
            List<OrderResponse> response = orderService.getOrdersByUserId(userId);
            List<OrderResponse> sorted = applySort(response, sort);
            List<OrderResponse> paged = slice(sorted, page, size);
            return ResponseEntity.ok(ApiResponse.success(paged, "Success", PaginationMeta.simple(page, size, sorted.size())));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("Provide either 'reference' or 'userId' query parameter."));
    }

    private List<OrderResponse> slice(List<OrderResponse> items, int page, int size) {
        if (items == null || items.isEmpty()) return items;
        if (size <= 0) return items;
        int from = Math.max(0, page) * size;
        if (from >= items.size()) return List.of();
        int to = Math.min(items.size(), from + size);
        return items.subList(from, to);
    }

    private List<OrderResponse> applySort(List<OrderResponse> items, String sort) {
        if (items == null) return List.of();
        if (sort == null || sort.isBlank()) return items;

        String[] parts = sort.split(",", 2);
        String field = parts[0].trim();
        String dir = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";
        boolean desc = "desc".equals(dir);

        return items.stream().sorted((a, b) -> {
            int cmp = 0;
            if ("createdAt".equals(field)) {
                LocalDateTime av = a.getCreatedAt();
                LocalDateTime bv = b.getCreatedAt();
                if (av == null && bv == null) cmp = 0;
                else if (av == null) cmp = 1;
                else if (bv == null) cmp = -1;
                else cmp = av.compareTo(bv);
            } else if ("status".equals(field)) {
                String av = a.getStatus();
                String bv = b.getStatus();
                if (av == null && bv == null) cmp = 0;
                else if (av == null) cmp = 1;
                else if (bv == null) cmp = -1;
                else cmp = av.compareToIgnoreCase(bv);
            } else {
                LocalDateTime av = a.getCreatedAt();
                LocalDateTime bv = b.getCreatedAt();
                if (av == null && bv == null) cmp = 0;
                else if (av == null) cmp = 1;
                else if (bv == null) cmp = -1;
                else cmp = av.compareTo(bv);
            }
            return desc ? -cmp : cmp;
        }).collect(Collectors.toList());
>>>>>>> 517cfdbabcad5678433bdd3ff85dacd99c0cfaeb
    }
}
