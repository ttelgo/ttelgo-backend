package com.tiktel.ttelgo.user.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.dto.PaginationMeta;
import com.tiktel.ttelgo.order.api.dto.OrderResponse;
import com.tiktel.ttelgo.order.application.OrderService;
import com.tiktel.ttelgo.user.api.dto.UpdateUserRequest;
import com.tiktel.ttelgo.user.api.dto.UserResponse;
import com.tiktel.ttelgo.user.application.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    private final UserService userService;
    private final OrderService orderService;
    
    @Autowired
    public UserController(UserService userService, OrderService orderService) {
        this.userService = userService;
        this.orderService = orderService;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * RESTful query-style lookup:
     * - GET /api/v1/users?email=user@example.com
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> findUser(@RequestParam(required = false) String email) {
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Provide 'email' query parameter."));
        }
        UserResponse response = userService.getUserByEmail(email.trim());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Nested resource:
     * - GET /api/v1/users/{id}/orders
     */
    @GetMapping("/{id}/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersForUser(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer size,
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort
    ) {
        List<OrderResponse> response = orderService.getOrdersByUserId(id);
        List<OrderResponse> sorted = applySort(response, sort);
        List<OrderResponse> paged = slice(sorted, page, size);
        return ResponseEntity.ok(ApiResponse.success(paged, "Success", PaginationMeta.simple(page, size, sorted.size())));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request) {
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
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
                // default: createdAt
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

