package com.tiktel.ttelgo.admin.api;

import com.tiktel.ttelgo.admin.api.dto.AdminDashboardResponse;
import com.tiktel.ttelgo.admin.application.AdminService;
import com.tiktel.ttelgo.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    private final AdminService adminService;
    
    @Autowired
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }
    
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        AdminDashboardResponse response = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

