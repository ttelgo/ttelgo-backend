package com.tiktel.ttelgo.admin.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    // User Stats
    private Long totalUsers;
    private Long activeUsers;
    private Long newUsersToday;
    private Long newUsersThisWeek;
    private Long newUsersThisMonth;
    
    // Order Stats
    private Long totalOrders;
    private Long pendingOrders;
    private Long completedOrders;
    private Long cancelledOrders;
    private BigDecimal totalRevenue;
    private BigDecimal revenueToday;
    private BigDecimal revenueThisWeek;
    private BigDecimal revenueThisMonth;
    
    // eSIM Stats
    private Long totalEsims;
    private Long activeEsims;
    private Long activatedToday;
    private Long activatedThisWeek;
    
    // API Stats
    private Long totalApiKeys;
    private Long activeApiKeys;
    private Long totalApiRequests;
    private Long apiRequestsToday;
    private Double averageApiResponseTime;
    
    // Recent Activity
    private List<RecentOrderDto> recentOrders;
    private List<RecentUserDto> recentUsers;
    private List<TopApiKeyDto> topApiKeys;
    
    // Charts Data
    private Map<String, Long> ordersByDay;
    private Map<String, Long> usersByDay;
    private Map<String, Long> revenueByDay;
    private Map<String, Long> apiRequestsByDay;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentOrderDto {
        private Long id;
        private String orderReference;
        private String customerEmail;
        private BigDecimal amount;
        private String status;
        private String createdAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentUserDto {
        private Long id;
        private String email;
        private String phone;
        private String createdAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopApiKeyDto {
        private Long id;
        private String keyName;
        private String customerEmail;
        private Long requestCount;
        private Double averageResponseTime;
    }
}
