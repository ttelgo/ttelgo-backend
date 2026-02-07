package com.tiktel.ttelgo.admin.application;

import com.tiktel.ttelgo.admin.api.dto.AdminDashboardResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

@Service
public class AdminService {

    /**
     * Returns dashboard stats. Minimal implementation so admin dashboard loads.
     * Can be extended later with real counts from repositories.
     */
    public AdminDashboardResponse getDashboardStats() {
        return AdminDashboardResponse.builder()
            .totalUsers(0L)
            .activeUsers(0L)
            .newUsersToday(0L)
            .newUsersThisWeek(0L)
            .newUsersThisMonth(0L)
            .totalOrders(0L)
            .pendingOrders(0L)
            .completedOrders(0L)
            .cancelledOrders(0L)
            .totalRevenue(BigDecimal.ZERO)
            .revenueToday(BigDecimal.ZERO)
            .revenueThisWeek(BigDecimal.ZERO)
            .revenueThisMonth(BigDecimal.ZERO)
            .totalEsims(0L)
            .activeEsims(0L)
            .activatedToday(0L)
            .activatedThisWeek(0L)
            .totalApiKeys(0L)
            .activeApiKeys(0L)
            .totalApiRequests(0L)
            .apiRequestsToday(0L)
            .averageApiResponseTime(0.0)
            .recentOrders(new ArrayList<>())
            .recentUsers(new ArrayList<>())
            .topApiKeys(new ArrayList<>())
            .ordersByDay(new HashMap<>())
            .usersByDay(new HashMap<>())
            .revenueByDay(new HashMap<>())
            .apiRequestsByDay(new HashMap<>())
            .build();
    }
}
