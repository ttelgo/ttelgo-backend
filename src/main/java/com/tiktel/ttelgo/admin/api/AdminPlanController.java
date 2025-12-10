package com.tiktel.ttelgo.admin.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.plan.api.dto.ListBundlesResponse;
import com.tiktel.ttelgo.plan.application.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/plans")
@RequiredArgsConstructor
public class AdminPlanController {
    
    private final PlanService planService;
    
    /**
     * Get all plans (loads all plans for client-side filtering/pagination)
     * GET /api/admin/plans/all
     * This endpoint loads all plans in chunks and returns them all for client-side processing
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<ListBundlesResponse>> getAllPlansComplete() {
        // Load all plans by making multiple paginated requests
        ListBundlesResponse allPlans = planService.loadAllPlans();
        return ResponseEntity.ok(ApiResponse.success(allPlans));
    }
    
    /**
     * Get all plans with pagination (legacy endpoint, kept for compatibility)
     * GET /api/admin/plans?page=1&perPage=20&search=term
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ListBundlesResponse>> getAllPlans(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer perPage,
            @RequestParam(required = false, defaultValue = "asc") String direction,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String search) {
        ListBundlesResponse response = planService.listAllBundlesPaginated(page, perPage, direction, orderBy, search);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Get plans by country with pagination
     * GET /api/admin/plans/country?countryIso=GB&page=1&perPage=50
     */
    @GetMapping("/country")
    public ResponseEntity<ApiResponse<ListBundlesResponse>> getPlansByCountry(
            @RequestParam String countryIso,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer perPage,
            @RequestParam(required = false, defaultValue = "asc") String direction,
            @RequestParam(required = false) String orderBy) {
        ListBundlesResponse response = planService.listBundlesByCountryPaginated(countryIso, page, perPage, direction, orderBy);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Get regional bundles filtered by region
     * GET /api/admin/plans/regional?region=Europe
     */
    @GetMapping("/regional")
    public ResponseEntity<ApiResponse<ListBundlesResponse>> getRegionalBundlesByRegion(
            @RequestParam(required = false) String region) {
        ListBundlesResponse response;
        if (region != null && !region.trim().isEmpty()) {
            response = planService.listRegionalBundlesByRegion(region.trim());
        } else {
            response = planService.listRegionalBundles();
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Get list of available regions from regional bundles
     * GET /api/admin/plans/regions
     */
    @GetMapping("/regions")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableRegions() {
        List<String> regions = planService.getAvailableRegions();
        return ResponseEntity.ok(ApiResponse.success(regions));
    }
}
