package com.tiktel.ttelgo.user.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.dto.PaginationMeta;
import com.tiktel.ttelgo.order.api.dto.OrderResponse;
import com.tiktel.ttelgo.order.api.mapper.OrderApiMapper;
import com.tiktel.ttelgo.order.application.OrderService;
import com.tiktel.ttelgo.user.api.dto.UpdateUserRequest;
import com.tiktel.ttelgo.user.api.dto.UserResponse;
import com.tiktel.ttelgo.user.application.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final OrderService orderService;
    private final OrderApiMapper orderApiMapper;

    @Autowired
    public UserController(UserService userService, OrderService orderService, OrderApiMapper orderApiMapper) {
        this.userService = userService;
        this.orderService = orderService;
        this.orderApiMapper = orderApiMapper;
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
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        var orders = orderService.getUserOrders(id, pageable);
        List<OrderResponse> response = orders.getContent().stream()
            .map(orderApiMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(response, "Success", PaginationMeta.simple(page, size, (int) orders.getTotalElements())));
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by(Sort.Direction.DESC, "createdAt");
        String[] parts = sort.split(",", 2);
        String field = parts[0].trim();
        String dir = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";
        return Sort.by("desc".equals(dir) ? Sort.Direction.DESC : Sort.Direction.ASC, field);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request) {
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}

