package com.tiktel.ttelgo.admin.api;

import com.tiktel.ttelgo.common.domain.enums.OrderStatus;
import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.dto.PaginationMeta;
import com.tiktel.ttelgo.order.api.dto.OrderResponse;
import com.tiktel.ttelgo.order.api.mapper.OrderApiMapper;
import com.tiktel.ttelgo.order.application.OrderService;
import com.tiktel.ttelgo.order.domain.Order;
import com.tiktel.ttelgo.order.infrastructure.mapper.OrderMapper;
import com.tiktel.ttelgo.order.infrastructure.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminOrderController {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderApiMapper orderApiMapper;
    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));

        Page<Order> orders;
        if (status != null && !status.isEmpty()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                orders = orderRepository.findByStatus(orderStatus, pageable).map(orderMapper::toDomain);
            } catch (IllegalArgumentException e) {
                orders = orderRepository.findAll(pageable).map(orderMapper::toDomain);
            }
        } else {
            orders = orderRepository.findAll(pageable).map(orderMapper::toDomain);
        }

        List<OrderResponse> orderResponses = orders.getContent().stream()
            .map(orderApiMapper::toResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(orderResponses, "Success", PaginationMeta.fromPage(orders)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(orderApiMapper.toResponse(order)));
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sort.split(",", 2);
        String field = parts[0].trim();
        String dir = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";
        Sort.Direction direction = "desc".equals(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }
}
