package com.tiktel.ttelgo.vendor.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.dto.PageRequest;
import com.tiktel.ttelgo.common.dto.PageResponse;
import com.tiktel.ttelgo.order.api.dto.CreateOrderRequest;
import com.tiktel.ttelgo.order.api.dto.OrderResponse;
import com.tiktel.ttelgo.order.api.mapper.OrderApiMapper;
import com.tiktel.ttelgo.order.application.OrderService;
import com.tiktel.ttelgo.order.domain.Order;
import com.tiktel.ttelgo.apikey.security.ApiClientResolver;
import com.tiktel.ttelgo.apikey.security.ApiScopeValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

/**
 * B2B Vendor Order API - Example of API Key secured endpoint.
 * 
 * This controller demonstrates:
 * - API key authentication (handled by ApiKeyAuthenticationFilter)
 * - Scope validation using ApiScopeValidator
 * - Client resolution using ApiClientResolver
 * 
 * Authentication: API Key required (Authorization: Api-Key {key} or X-API-Key: {key})
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/vendor/orders")
@SecurityRequirement(name = "API Key Authentication")
@Tag(name = "Vendor Orders (B2B)", description = "Vendor order management - API Key secured")
public class VendorOrderController {
    
    private final OrderService orderService;
    private final OrderApiMapper orderApiMapper;
    private final ApiClientResolver apiClientResolver;
    private final ApiScopeValidator scopeValidator;
    
    public VendorOrderController(
            OrderService orderService, 
            OrderApiMapper orderApiMapper,
            ApiClientResolver apiClientResolver,
            ApiScopeValidator scopeValidator) {
        this.orderService = orderService;
        this.orderApiMapper = orderApiMapper;
        this.apiClientResolver = apiClientResolver;
        this.scopeValidator = scopeValidator;
    }
    
    /**
     * Create vendor order - Example of API key secured endpoint with scope validation.
     * 
     * Required scope: POST:/api/v1/vendor/orders
     */
    @Operation(summary = "Create vendor order", description = "Create a new order for vendor (API Key required)")
    @PostMapping
    public ApiResponse<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Creating vendor order: bundleCode={}, quantity={}",
                request.getBundleCode(), request.getQuantity());
        
        // API key is automatically validated by ApiKeyAuthenticationFilter
        // Get API client information
        Long apiKeyId = apiClientResolver.getCurrentApiClientId();
        if (apiKeyId == null) {
            throw new AccessDeniedException("API key authentication required");
        }
        
        // Validate scope for this endpoint
        String endpoint = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        if (!scopeValidator.validateCurrentScope(endpoint, method)) {
            log.warn("API key {} attempted to access endpoint {} without proper scope", 
                    apiKeyId, endpoint);
            throw new AccessDeniedException("API key does not have permission to access this endpoint");
        }
        
        // Extract vendor ID from API key (you may need to link API key to vendor in your domain)
        // For now, using API key ID as vendor identifier
        Long vendorId = apiKeyId; // TODO: Map API key to vendor ID if needed
        
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        
        Order order = orderService.createB2BOrder(
                vendorId,
                request.getBundleCode(),
                request.getQuantity(),
                ipAddress,
                userAgent
        );
        
        log.info("Vendor order created successfully: orderId={}, apiKeyId={}", 
                order.getId(), apiKeyId);
        
        return ApiResponse.success(orderApiMapper.toResponse(order));
    }
    
    /**
     * Get vendor orders - Example of API key secured endpoint with scope validation.
     * 
     * Required scope: GET:/api/v1/vendor/orders
     */
    @Operation(summary = "Get vendor orders", description = "Get all orders for vendor (API Key required)")
    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> getOrders(
            @ModelAttribute PageRequest pageRequest,
            HttpServletRequest httpRequest) {
        
        // Get API client information
        Long apiKeyId = apiClientResolver.getCurrentApiClientId();
        if (apiKeyId == null) {
            throw new AccessDeniedException("API key authentication required");
        }
        
        // Validate scope
        String endpoint = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        if (!scopeValidator.validateCurrentScope(endpoint, method)) {
            throw new AccessDeniedException("API key does not have permission to access this endpoint");
        }
        
        Long vendorId = apiKeyId; // TODO: Map API key to vendor ID if needed
        log.info("Getting orders for vendor: {}, apiKeyId: {}", vendorId, apiKeyId);
        
        Page<Order> orders = orderService.getVendorOrders(vendorId, pageRequest.toPageable("createdAt"));
        PageResponse<OrderResponse> response = PageResponse.of(
                orders.getContent().stream().map(orderApiMapper::toResponse).toList(),
                orders
        );
        
        return ApiResponse.success(response);
    }
    
    /**
     * Get order details - Example of API key secured endpoint.
     * 
     * Required scope: GET:/api/v1/vendor/orders/{id}
     */
    @Operation(summary = "Get order details", description = "Get details of a specific order (API Key required)")
    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(
            @PathVariable Long orderId,
            HttpServletRequest httpRequest) {
        
        // Get API client information
        Long apiKeyId = apiClientResolver.getCurrentApiClientId();
        if (apiKeyId == null) {
            throw new AccessDeniedException("API key authentication required");
        }
        
        // Validate scope
        String endpoint = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        if (!scopeValidator.validateCurrentScope(endpoint, method)) {
            throw new AccessDeniedException("API key does not have permission to access this endpoint");
        }
        
        Long vendorId = apiKeyId; // TODO: Map API key to vendor ID if needed
        Order order = orderService.getOrderById(orderId);
        
        // Verify order belongs to vendor
        if (!order.getVendorId().equals(vendorId)) {
            throw new AccessDeniedException("Unauthorized: Order does not belong to this vendor");
        }
        
        return ApiResponse.success(orderApiMapper.toResponse(order));
    }
}

