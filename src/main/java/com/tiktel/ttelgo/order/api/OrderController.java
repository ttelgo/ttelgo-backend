package com.tiktel.ttelgo.order.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.dto.PaginationMeta;
import com.tiktel.ttelgo.order.api.dto.CreateOrderRequest;
import com.tiktel.ttelgo.order.api.dto.OrderResponse;
import com.tiktel.ttelgo.order.application.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    
    private final OrderService orderService;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    /**
     * Create B2C order
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        Long userId = extractUserId(authentication);
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        
        com.tiktel.ttelgo.order.domain.Order order = orderService.createB2COrder(
                userId,
                request.getCustomerEmail(),
                request.getBundleCode(),
                request.getQuantity(),
                ipAddress,
                userAgent != null ? userAgent : "Unknown"
        );
        
        OrderResponse response = OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .bundleCode(order.getBundleCode())
                .bundleName(order.getBundleName())
                .quantity(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .countryIso(order.getCountryIso())
                .dataAmount(order.getDataAmount() != null ? order.getDataAmount().toString() : null)
                .validityDays(order.getValidityDays())
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .completedAt(order.getCompletedAt())
                .errorMessage(order.getErrorMessage())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    private Long extractUserId(Authentication authentication) {
        // TODO: Extract user ID from JWT token
        if (authentication == null) {
            return null; // Guest checkout
        }
        return 1L; // Placeholder
    }
    
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
                String av = a.getStatus() != null ? a.getStatus().name() : null;
                String bv = b.getStatus() != null ? b.getStatus().name() : null;
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
    }
}
