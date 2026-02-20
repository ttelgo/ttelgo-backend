package com.tiktel.ttelgo.order.infrastructure.adapter;

import com.tiktel.ttelgo.order.application.port.OrderRepositoryPort;
import com.tiktel.ttelgo.order.domain.Order;
import com.tiktel.ttelgo.order.infrastructure.mapper.OrderMapper;
import com.tiktel.ttelgo.order.infrastructure.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OrderRepositoryAdapter implements OrderRepositoryPort {
    
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    public OrderRepositoryAdapter(OrderRepository orderRepository, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }
    
    @Override
    @Transactional
    public Order save(Order order) {
        var entity = orderMapper.toEntity(order);
        
        // Check if order already exists (has an ID)
        if (order.getId() != null && orderRepository.existsById(order.getId())) {
            // UPDATE existing order
            String updateSql = "UPDATE orders SET " +
                               "user_id = :userId, " +
                               "vendor_id = :vendorId, " +
                               "customer_email = :customerEmail, " +
                               "bundle_code = :bundleCode, " +
                               "bundle_name = :bundleName, " +
                               "quantity = :quantity, " +
                               "unit_price = :unitPrice, " +
                               "total_amount = :totalAmount, " +
                               "currency = :currency, " +
                               "esimgo_order_id = :esimgoOrderId, " +
                               "status = CAST(:status AS order_status), " +
                               "payment_status = CAST(:paymentStatus AS payment_status), " +
                               "country_iso = :countryIso, " +
                               "data_amount = :dataAmount, " +
                               "validity_days = :validityDays, " +
                               "ip_address = :ipAddress, " +
                               "user_agent = :userAgent, " +
                               "updated_at = :updatedAt, " +
                               "retry_count = :retryCount, " +
                               "paid_at = COALESCE(:paidAt, paid_at), " +
                               "provisioned_at = COALESCE(:provisionedAt, provisioned_at), " +
                               "completed_at = COALESCE(:completedAt, completed_at) " +
                               "WHERE id = :id";
            
            entityManager.createNativeQuery(updateSql)
                    .setParameter("userId", entity.getUserId())
                    .setParameter("vendorId", entity.getVendorId())
                    .setParameter("customerEmail", entity.getCustomerEmail())
                    .setParameter("bundleCode", entity.getBundleCode())
                    .setParameter("bundleName", entity.getBundleName())
                    .setParameter("quantity", entity.getQuantity())
                    .setParameter("unitPrice", entity.getUnitPrice())
                    .setParameter("totalAmount", entity.getTotalAmount())
                    .setParameter("currency", entity.getCurrency())
                    .setParameter("esimgoOrderId", entity.getEsimgoOrderId())
                    .setParameter("status", entity.getStatus() != null ? entity.getStatus().name() : "ORDER_CREATED")
                    .setParameter("paymentStatus", entity.getPaymentStatus() != null ? entity.getPaymentStatus().name() : "CREATED")
                    .setParameter("countryIso", entity.getCountryIso())
                    .setParameter("dataAmount", entity.getDataAmount())
                    .setParameter("validityDays", entity.getValidityDays())
                    .setParameter("ipAddress", entity.getIpAddress())
                    .setParameter("userAgent", entity.getUserAgent())
                    .setParameter("updatedAt", java.time.LocalDateTime.now())
                    .setParameter("retryCount", entity.getRetryCount() != null ? entity.getRetryCount() : 0)
                    .setParameter("paidAt", entity.getPaidAt())
                    .setParameter("provisionedAt", entity.getProvisionedAt())
                    .setParameter("completedAt", entity.getCompletedAt())
                    .setParameter("id", entity.getId())
                    .executeUpdate();
            
            // Fetch the updated entity from database
            var updatedEntity = orderRepository.findById(entity.getId())
                    .orElseThrow(() -> new RuntimeException("Failed to retrieve updated order"));
            return orderMapper.toDomain(updatedEntity);
        } else {
            // INSERT new order
            String insertSql = "INSERT INTO orders (order_number, user_id, vendor_id, customer_email, bundle_code, bundle_name, quantity, unit_price, total_amount, currency, esimgo_order_id, status, payment_status, country_iso, data_amount, validity_days, ip_address, user_agent, created_at, updated_at, retry_count) " +
                              "VALUES (:orderNumber, :userId, :vendorId, :customerEmail, :bundleCode, :bundleName, :quantity, :unitPrice, :totalAmount, :currency, :esimgoOrderId, CAST(:status AS order_status), CAST(:paymentStatus AS payment_status), :countryIso, :dataAmount, :validityDays, :ipAddress, :userAgent, :createdAt, :updatedAt, :retryCount) RETURNING id";
            
            Long id = (Long) entityManager.createNativeQuery(insertSql)
                    .setParameter("orderNumber", entity.getOrderNumber())
                    .setParameter("userId", entity.getUserId())
                    .setParameter("vendorId", entity.getVendorId())
                    .setParameter("customerEmail", entity.getCustomerEmail())
                    .setParameter("bundleCode", entity.getBundleCode())
                    .setParameter("bundleName", entity.getBundleName())
                    .setParameter("quantity", entity.getQuantity())
                    .setParameter("unitPrice", entity.getUnitPrice())
                    .setParameter("totalAmount", entity.getTotalAmount())
                    .setParameter("currency", entity.getCurrency())
                    .setParameter("esimgoOrderId", entity.getEsimgoOrderId())
                    .setParameter("status", entity.getStatus() != null ? entity.getStatus().name() : "ORDER_CREATED")
                    .setParameter("paymentStatus", entity.getPaymentStatus() != null ? entity.getPaymentStatus().name() : "CREATED")
                    .setParameter("countryIso", entity.getCountryIso())
                    .setParameter("dataAmount", entity.getDataAmount())
                    .setParameter("validityDays", entity.getValidityDays())
                    .setParameter("ipAddress", entity.getIpAddress())
                    .setParameter("userAgent", entity.getUserAgent())
                    .setParameter("createdAt", entity.getCreatedAt())
                    .setParameter("updatedAt", entity.getUpdatedAt())
                    .setParameter("retryCount", entity.getRetryCount() != null ? entity.getRetryCount() : 0)
                    .getSingleResult();
            
            // Fetch the saved entity
            var savedEntity = orderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Failed to retrieve saved order"));
            return orderMapper.toDomain(savedEntity);
        }
    }
    
    @Override
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id)
                .map(orderMapper::toDomain);
    }
    
    @Override
    public Optional<Order> findByOrderReference(String orderReference) {
        // orderReference is an alias for orderNumber
        return orderRepository.findByOrderNumber(orderReference)
                .map(orderMapper::toDomain);
    }
    
    @Override
    public List<Order> findByUserId(Long userId) {
        return orderRepository.findByUserId(userId, org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .stream()
                .map(orderMapper::toDomain)
                .collect(Collectors.toList());
    }
}

