package com.tiktel.ttelgo.admin.application;

import com.tiktel.ttelgo.admin.api.dto.AdminDashboardResponse;
import com.tiktel.ttelgo.apikey.domain.ApiKey;
import com.tiktel.ttelgo.apikey.infrastructure.repository.ApiKeyRepository;
import com.tiktel.ttelgo.apikey.infrastructure.repository.ApiUsageLogRepository;
import com.tiktel.ttelgo.common.domain.enums.EsimStatus;
import com.tiktel.ttelgo.esim.infrastructure.repository.EsimJpaEntity;
import com.tiktel.ttelgo.esim.infrastructure.repository.EsimRepository;
import com.tiktel.ttelgo.order.infrastructure.repository.OrderJpaEntity;
import com.tiktel.ttelgo.order.infrastructure.repository.OrderRepository;
import com.tiktel.ttelgo.user.domain.User;
import com.tiktel.ttelgo.user.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {
    
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final EsimRepository esimRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiUsageLogRepository apiUsageLogRepository;
    
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboardStats() {
        log.info("Generating admin dashboard statistics");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime weekStart = now.minusWeeks(1);
        LocalDateTime monthStart = now.minusMonths(1);
        
        // User Stats
        long totalUsers = userRepository.count();
        long newUsersToday = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(todayStart))
                .count();
        long newUsersThisWeek = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(weekStart))
                .count();
        long newUsersThisMonth = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(monthStart))
                .count();
        long activeUsers = totalUsers; // Simplified - could be users with recent activity
        
        // Order Stats
        List<OrderJpaEntity> allOrders = orderRepository.findAll();
        long totalOrders = allOrders.size();
        long pendingOrders = allOrders.stream()
                .filter(o -> o.getStatus() == com.tiktel.ttelgo.common.domain.enums.OrderStatus.ORDER_CREATED 
                        || o.getStatus() == com.tiktel.ttelgo.common.domain.enums.OrderStatus.PAYMENT_PENDING
                        || o.getStatus() == com.tiktel.ttelgo.common.domain.enums.OrderStatus.PAYMENT_PROCESSING
                        || o.getStatus() == com.tiktel.ttelgo.common.domain.enums.OrderStatus.PROVISIONING
                        || o.getStatus() == com.tiktel.ttelgo.common.domain.enums.OrderStatus.PENDING_SYNC)
                .count();
        long completedOrders = allOrders.stream()
                .filter(o -> o.getStatus() == com.tiktel.ttelgo.common.domain.enums.OrderStatus.COMPLETED)
                .count();
        long cancelledOrders = allOrders.stream()
                .filter(o -> o.getStatus() == com.tiktel.ttelgo.common.domain.enums.OrderStatus.CANCELED 
                        || o.getStatus() == com.tiktel.ttelgo.common.domain.enums.OrderStatus.REFUNDED)
                .count();
        
        BigDecimal totalRevenue = allOrders.stream()
                .filter(o -> o.getTotalAmount() != null && o.getStatus() == com.tiktel.ttelgo.common.domain.enums.OrderStatus.COMPLETED)
                .map(OrderJpaEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal revenueToday = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(todayStart) 
                        && o.getTotalAmount() != null && o.getStatus() == com.tiktel.ttelgo.common.domain.enums.OrderStatus.COMPLETED)
                .map(OrderJpaEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal revenueThisWeek = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(weekStart)
                        && o.getTotalAmount() != null && o.getStatus() == com.tiktel.ttelgo.common.domain.enums.OrderStatus.COMPLETED)
                .map(OrderJpaEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal revenueThisMonth = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(monthStart)
                        && o.getTotalAmount() != null && o.getStatus() == com.tiktel.ttelgo.common.domain.enums.OrderStatus.COMPLETED)
                .map(OrderJpaEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // eSIM Stats
        List<EsimJpaEntity> allEsims = esimRepository.findAll();
        long totalEsims = allEsims.size();
        long activeEsims = allEsims.stream()
                .filter(e -> e.getStatus() == EsimStatus.ACTIVE)
                .count();
        long activatedToday = allEsims.stream()
                .filter(e -> e.getActivatedAt() != null && e.getActivatedAt().isAfter(todayStart))
                .count();
        long activatedThisWeek = allEsims.stream()
                .filter(e -> e.getActivatedAt() != null && e.getActivatedAt().isAfter(weekStart))
                .count();
        
        // API Stats
        long totalApiKeys = apiKeyRepository.count();
        long activeApiKeys = apiKeyRepository.findByIsActiveTrue().size();
        long totalApiRequests = apiUsageLogRepository.count();
        long apiRequestsToday = apiUsageLogRepository.countTotalRequestsSince(todayStart);
        
        // Calculate average response time (simplified)
        Double averageApiResponseTime = apiUsageLogRepository.findAll().stream()
                .filter(log -> log.getResponseTimeMs() != null)
                .mapToDouble(log -> log.getResponseTimeMs().doubleValue())
                .average()
                .orElse(0.0);
        
        // Recent Orders
        List<AdminDashboardResponse.RecentOrderDto> recentOrders = orderRepository.findAll(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream()
                .map(order -> AdminDashboardResponse.RecentOrderDto.builder()
                        .id(order.getId())
                        .orderReference(order.getOrderNumber() != null ? order.getOrderNumber() : String.valueOf(order.getId()))
                        .customerEmail(order.getCustomerEmail())
                        .amount(order.getTotalAmount())
                        .status(order.getStatus() != null ? order.getStatus().name() : "UNKNOWN")
                        .createdAt(order.getCreatedAt() != null ? 
                                order.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                        .build())
                .collect(Collectors.toList());
        
        // Recent Users
        List<AdminDashboardResponse.RecentUserDto> recentUsers = userRepository.findAll(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(user -> AdminDashboardResponse.RecentUserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .createdAt(user.getCreatedAt() != null ? 
                                user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                        .build())
                .collect(Collectors.toList());
        
        // Top API Keys
        List<AdminDashboardResponse.TopApiKeyDto> topApiKeys = new ArrayList<>();
        List<Object[]> topApiKeysData = apiUsageLogRepository.getTopApiKeysByUsage(weekStart);
        for (Object[] data : topApiKeysData) {
            Long apiKeyId = (Long) data[0];
            Long requestCount = (Long) data[1];
            apiKeyRepository.findById(apiKeyId).ifPresent(apiKey -> {
                Double avgResponseTime = apiUsageLogRepository.getAverageResponseTime(apiKeyId, weekStart);
                topApiKeys.add(AdminDashboardResponse.TopApiKeyDto.builder()
                        .id(apiKeyId)
                        .keyName(apiKey.getKeyName())
                        .customerEmail(apiKey.getCustomerEmail())
                        .requestCount(requestCount)
                        .averageResponseTime(avgResponseTime)
                        .build());
            });
        }
        
        // Charts Data (simplified - last 7 days)
        Map<String, Long> ordersByDay = new HashMap<>();
        Map<String, Long> usersByDay = new HashMap<>();
        Map<String, Long> revenueByDay = new HashMap<>();
        Map<String, Long> apiRequestsByDay = new HashMap<>();
        
        for (int i = 6; i >= 0; i--) {
            LocalDateTime dayStart = now.minusDays(i).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime dayEnd = dayStart.plusDays(1);
            String dayKey = dayStart.format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            long ordersCount = allOrders.stream()
                    .filter(o -> o.getCreatedAt() != null && 
                            o.getCreatedAt().isAfter(dayStart) && o.getCreatedAt().isBefore(dayEnd))
                    .count();
            ordersByDay.put(dayKey, ordersCount);
            
            long usersCount = userRepository.findAll().stream()
                    .filter(u -> u.getCreatedAt() != null && 
                            u.getCreatedAt().isAfter(dayStart) && u.getCreatedAt().isBefore(dayEnd))
                    .count();
            usersByDay.put(dayKey, usersCount);
            
            BigDecimal dayRevenue = allOrders.stream()
                    .filter(o -> o.getCreatedAt() != null && 
                            o.getCreatedAt().isAfter(dayStart) && o.getCreatedAt().isBefore(dayEnd)
                            && o.getTotalAmount() != null && o.getStatus() == com.tiktel.ttelgo.common.domain.enums.OrderStatus.COMPLETED)
                    .map(OrderJpaEntity::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            revenueByDay.put(dayKey, dayRevenue.longValue());
            
            long apiRequestsCount = apiUsageLogRepository.countTotalRequestsSince(dayStart);
            apiRequestsByDay.put(dayKey, apiRequestsCount);
        }
        
        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
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
                .averageApiResponseTime(averageApiResponseTime)
                .recentOrders(recentOrders)
                .recentUsers(recentUsers)
                .topApiKeys(topApiKeys)
                .ordersByDay(ordersByDay)
                .usersByDay(usersByDay)
                .revenueByDay(revenueByDay)
                .apiRequestsByDay(apiRequestsByDay)
                .build();
    }
}

