package com.tiktel.ttelgo.order.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.dto.PaginationMeta;
import com.tiktel.ttelgo.order.api.dto.OrderResponse;
import com.tiktel.ttelgo.order.application.OrderService;
import org.springframework.http.ResponseEntity;
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
    }
}
