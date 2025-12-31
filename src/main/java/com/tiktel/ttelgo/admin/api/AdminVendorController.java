package com.tiktel.ttelgo.admin.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.dto.PageRequest;
import com.tiktel.ttelgo.common.dto.PageResponse;
import com.tiktel.ttelgo.vendor.application.VendorService;
import com.tiktel.ttelgo.vendor.domain.Vendor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Admin API for vendor management
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/vendors")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Vendors", description = "Vendor management (Admin only)")
public class AdminVendorController {
    
    private final VendorService vendorService;
    
    public AdminVendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }
    
    @Operation(summary = "Get all vendors", description = "Get all vendors with pagination")
    @GetMapping
    public ApiResponse<PageResponse<Vendor>> getAllVendors(@ModelAttribute PageRequest pageRequest) {
        log.info("Getting all vendors");
        
        Page<Vendor> vendors = vendorService.getAllVendors(pageRequest.toPageable("createdAt"));
        PageResponse<Vendor> response = PageResponse.of(vendors.getContent(), vendors);
        
        return ApiResponse.success(response);
    }
    
    @Operation(summary = "Get vendor details", description = "Get details of a specific vendor")
    @GetMapping("/{vendorId}")
    public ApiResponse<Vendor> getVendor(@PathVariable Long vendorId) {
        log.info("Getting vendor: {}", vendorId);
        Vendor vendor = vendorService.getVendorById(vendorId);
        return ApiResponse.success(vendor);
    }
    
    @Operation(summary = "Create vendor", description = "Create a new vendor")
    @PostMapping
    public ApiResponse<Vendor> createVendor(
            @Valid @RequestBody Vendor vendor,
            Authentication authentication) {
        
        Long adminId = extractUserId(authentication);
        log.info("Creating vendor: email={}", vendor.getEmail());
        
        Vendor created = vendorService.createVendor(vendor, adminId);
        return ApiResponse.success(created);
    }
    
    @Operation(summary = "Approve vendor", description = "Approve a pending vendor")
    @PostMapping("/{vendorId}/approve")
    public ApiResponse<Vendor> approveVendor(
            @PathVariable Long vendorId,
            Authentication authentication) {
        
        Long adminId = extractUserId(authentication);
        log.info("Approving vendor: vendorId={}", vendorId);
        
        Vendor approved = vendorService.approveVendor(vendorId, adminId);
        return ApiResponse.success(approved);
    }
    
    @Operation(summary = "Suspend vendor", description = "Suspend a vendor account")
    @PostMapping("/{vendorId}/suspend")
    public ApiResponse<Vendor> suspendVendor(
            @PathVariable Long vendorId,
            @RequestParam String reason,
            Authentication authentication) {
        
        Long adminId = extractUserId(authentication);
        log.info("Suspending vendor: vendorId={}, reason={}", vendorId, reason);
        
        Vendor suspended = vendorService.suspendVendor(vendorId, reason, adminId);
        return ApiResponse.success(suspended);
    }
    
    @Operation(summary = "Adjust vendor balance", description = "Adjust vendor wallet or credit balance")
    @PostMapping("/{vendorId}/adjust-balance")
    public ApiResponse<String> adjustBalance(
            @PathVariable Long vendorId,
            @RequestParam BigDecimal amount,
            @RequestParam String reason,
            Authentication authentication) {
        
        Long adminId = extractUserId(authentication);
        log.info("Adjusting vendor balance: vendorId={}, amount={}", vendorId, amount);
        
        vendorService.adjustBalance(vendorId, amount, reason, adminId);
        return ApiResponse.success("Balance adjusted successfully");
    }
    
    private Long extractUserId(Authentication authentication) {
        // TODO: Extract user ID from JWT token
        return 1L; // Placeholder
    }
}

