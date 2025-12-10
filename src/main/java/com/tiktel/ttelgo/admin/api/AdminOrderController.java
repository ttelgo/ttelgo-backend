package com.tiktel.ttelgo.admin.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.order.api.dto.OrderResponse;
import com.tiktel.ttelgo.order.application.OrderService;
import com.tiktel.ttelgo.order.domain.Order;
import com.tiktel.ttelgo.order.infrastructure.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {
    
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer size,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<Order> orders;
        if (status != null && !status.isEmpty()) {
            try {
                com.tiktel.ttelgo.order.domain.OrderStatus orderStatus = 
                    com.tiktel.ttelgo.order.domain.OrderStatus.valueOf(status.toUpperCase());
                orders = orderRepository.findByStatus(orderStatus, pageable);
            } catch (IllegalArgumentException e) {
                orders = orderRepository.findAll(pageable);
            }
        } else {
            orders = orderRepository.findAll(pageable);
        }
        
        List<OrderResponse> orderResponses = orders.getContent().stream()
            .map(order -> orderService.getOrderById(order.getId()))
            .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(orderResponses));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

