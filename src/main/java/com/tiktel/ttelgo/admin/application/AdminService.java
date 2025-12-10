package com.tiktel.ttelgo.admin.application;

import com.tiktel.ttelgo.admin.api.dto.AdminDashboardResponse;
import com.tiktel.ttelgo.apikey.infrastructure.repository.ApiKeyRepository;
import com.tiktel.ttelgo.apikey.infrastructure.repository.ApiUsageLogRepository;
import com.tiktel.ttelgo.esim.infrastructure.repository.EsimRepository;
import com.tiktel.ttelgo.order.domain.Order;
import com.tiktel.ttelgo.order.domain.OrderStatus;
import com.tiktel.ttelgo.order.domain.PaymentStatus;
import com.tiktel.ttelgo.order.infrastructure.repository.OrderRepository;
import com.tiktel.ttelgo.user.domain.User;
import com.tiktel.ttelgo.user.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {
    
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final EsimRepository esimRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiUsageLogRepository apiUsageLogRepository;
    
    public AdminDashboardResponse getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime weekStart = now.minusWeeks(1);
        LocalDateTime monthStart = now.minusMonths(1);
        
        // User Stats
        long totalUsers = userRepository.count();
        long newUsersToday = userRepository.findAll().stream()
            .filter(user -> user.getCreatedAt() != null && user.getCreatedAt().isAfter(todayStart))
            .count();
        long newUsersThisWeek = userRepository.findAll().stream()
            .filter(user -> user.getCreatedAt() != null && user.getCreatedAt().isAfter(weekStart))
            .count();
        long newUsersThisMonth = userRepository.findAll().stream()
            .filter(user -> user.getCreatedAt() != null && user.getCreatedAt().isAfter(monthStart))
            .count();
        
        // Order Stats
        List<Order> allOrders = orderRepository.findAll();
        long totalOrders = allOrders.size();
        long pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING).size();
        long completedOrders = orderRepository.findByStatus(OrderStatus.COMPLETED).size();
        long cancelledOrders = orderRepository.findByStatus(OrderStatus.CANCELLED).size();
        
        BigDecimal totalRevenue = allOrders.stream()
            .filter(order -> order.getPaymentStatus() == PaymentStatus.SUCCESS)
            .map(order -> order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal revenueToday = allOrders.stream()
            .filter(order -> order.getPaymentStatus() == PaymentStatus.SUCCESS)
            .filter(order -> order.getCreatedAt() != null && order.getCreatedAt().isAfter(todayStart))
            .map(order -> order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal revenueThisWeek = allOrders.stream()
            .filter(order -> order.getPaymentStatus() == PaymentStatus.SUCCESS)
            .filter(order -> order.getCreatedAt() != null && order.getCreatedAt().isAfter(weekStart))
            .map(order -> order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal revenueThisMonth = allOrders.stream()
            .filter(order -> order.getPaymentStatus() == PaymentStatus.SUCCESS)
            .filter(order -> order.getCreatedAt() != null && order.getCreatedAt().isAfter(monthStart))
            .map(order -> order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // eSIM Stats
        long totalEsims = esimRepository.count();
        long activeEsims = esimRepository.findAll().stream()
            .filter(esim -> esim.getStatus() != null && 
                   (esim.getStatus().name().equals("ACTIVATED") || esim.getStatus().name().equals("PROVISIONED")))
            .count();
        
        long activatedToday = esimRepository.findAll().stream()
            .filter(esim -> esim.getActivatedAt() != null && esim.getActivatedAt().isAfter(todayStart))
            .count();
        
        long activatedThisWeek = esimRepository.findAll().stream()
            .filter(esim -> esim.getActivatedAt() != null && esim.getActivatedAt().isAfter(weekStart))
            .count();
        
        // API Stats
        long totalApiKeys = apiKeyRepository.count();
        long activeApiKeys = apiKeyRepository.countActiveKeys();
        long totalApiRequests = apiUsageLogRepository.countTotalRequestsSince(monthStart);
        long apiRequestsToday = apiUsageLogRepository.countTotalRequestsSince(todayStart);
        
        // Recent Orders
        List<Order> recentOrders = orderRepository.findAll(
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
        
        List<AdminDashboardResponse.RecentOrderDto> recentOrderDtos = recentOrders.stream()
            .map(order -> AdminDashboardResponse.RecentOrderDto.builder()
                .id(order.getId())
                .orderReference(order.getOrderReference())
                .customerEmail(order.getUserId() != null ? "User #" + order.getUserId() : "N/A")
                .amount(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO)
                .status(order.getStatus() != null ? order.getStatus().name() : "UNKNOWN")
                .createdAt(order.getCreatedAt() != null ? 
                    order.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "")
                .build())
            .collect(Collectors.toList());
        
        // Recent Users
        List<User> recentUsers = userRepository.findAll(
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
        
        List<AdminDashboardResponse.RecentUserDto> recentUserDtos = recentUsers.stream()
            .map(user -> AdminDashboardResponse.RecentUserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt() != null ? 
                    user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "")
                .build())
            .collect(Collectors.toList());
        
        // Top API Keys
        List<Object[]> topApiKeysData = apiUsageLogRepository.getTopApiKeysByUsage(monthStart);
        List<AdminDashboardResponse.TopApiKeyDto> topApiKeyDtos = topApiKeysData.stream()
            .limit(5)
            .map(arr -> {
                Long apiKeyId = (Long) arr[0];
                Long count = (Long) arr[1];
                return apiKeyRepository.findById(apiKeyId)
                    .map(key -> AdminDashboardResponse.TopApiKeyDto.builder()
                        .id(key.getId())
                        .keyName(key.getKeyName())
                        .customerEmail(key.getCustomerEmail())
                        .requestCount(count)
                        .averageResponseTime(0.0) // Can be calculated if needed
                        .build())
                    .orElse(null);
            })
            .filter(dto -> dto != null)
            .collect(Collectors.toList());
        
        return AdminDashboardResponse.builder()
            .totalUsers(totalUsers)
            .activeUsers(totalUsers) // Can be enhanced with last login tracking
            .newUsersToday(newUsersToday)
            .newUsersThisWeek(newUsersThisWeek)
            .newUsersThisMonth(newUsersThisMonth)
            .totalOrders(totalOrders)
            .pendingOrders(pendingOrders)
            .completedOrders(completedOrders)
            .cancelledOrders(cancelledOrders)
            .totalRevenue(totalRevenue)
            .revenueToday(revenueToday)
            .revenueThisWeek(revenueThisWeek)
            .revenueThisMonth(revenueThisMonth)
            .totalEsims(totalEsims)
            .activeEsims(activeEsims)
            .activatedToday(activatedToday)
            .activatedThisWeek(activatedThisWeek)
            .totalApiKeys(totalApiKeys)
            .activeApiKeys(activeApiKeys)
            .totalApiRequests(totalApiRequests)
            .apiRequestsToday(apiRequestsToday)
            .averageApiResponseTime(0.0) // Can be calculated
            .recentOrders(recentOrderDtos)
            .recentUsers(recentUserDtos)
            .topApiKeys(topApiKeyDtos)
            .ordersByDay(new HashMap<>()) // Can be populated with daily stats
            .usersByDay(new HashMap<>())
            .revenueByDay(new HashMap<>())
            .apiRequestsByDay(new HashMap<>())
            .build();
    }
}
