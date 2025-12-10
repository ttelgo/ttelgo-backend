package com.tiktel.ttelgo.order.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.order.api.dto.OrderResponse;
import com.tiktel.ttelgo.order.application.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private final OrderService orderService;
    
    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/reference/{orderReference}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByReference(
            @PathVariable String orderReference) {
        OrderResponse response = orderService.getOrderByReference(orderReference);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByUserId(
            @PathVariable Long userId) {
        List<OrderResponse> response = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

