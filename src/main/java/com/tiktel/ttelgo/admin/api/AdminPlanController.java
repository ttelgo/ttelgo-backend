package com.tiktel.ttelgo.admin.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.dto.PaginationMeta;
import com.tiktel.ttelgo.plan.api.dto.ListBundlesResponse;
import com.tiktel.ttelgo.plan.application.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/bundles")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminPlanController {
    
    private final PlanService planService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<ListBundlesResponse>> getAllPlans(
            @RequestParam(required = false, defaultValue = "false") Boolean all,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "20") Integer perPage,
            @RequestParam(required = false, defaultValue = "name,asc") String sort,
            @RequestParam(required = false, defaultValue = "asc") String direction,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String countryIso,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String region
    ) {
        int p = page != null ? page : 0;
        int effectivePerPage = size != null ? size : (perPage != null ? perPage : 20);
        SortParts sortParts = parseSort(sort);
        String effectiveOrderBy = orderBy != null ? orderBy : sortParts.field;
        String effectiveDirection = direction != null ? direction : sortParts.direction;

        if (Boolean.TRUE.equals(all)) {
            // Load all plans by making multiple paginated requests (client-side filtering in admin UI)
            return ResponseEntity.ok(ApiResponse.success(planService.loadAllPlans()));
        }

        if (countryIso != null && !countryIso.trim().isEmpty()) {
            ListBundlesResponse response = planService.listBundlesByCountryPaginated(countryIso.trim(), p + 1, effectivePerPage, effectiveDirection, effectiveOrderBy);
            return ResponseEntity.ok(ApiResponse.success(response, "Success", PaginationMeta.simple(p, effectivePerPage, totalBundles(response))));
        }

        if (type != null && !type.trim().isEmpty()) {
            String t = type.trim().toLowerCase();
            ListBundlesResponse response = switch (t) {
                case "regional" -> (region != null && !region.trim().isEmpty())
                        ? planService.listRegionalBundlesByRegion(region.trim())
                        : planService.listRegionalBundles();
                case "local" -> planService.listLocalBundles();
                case "global" -> planService.listGlobalBundles();
                default -> planService.listAllBundlesPaginated(p + 1, effectivePerPage, effectiveDirection, effectiveOrderBy, search);
            };
            return ResponseEntity.ok(ApiResponse.success(response, "Success", PaginationMeta.simple(p, effectivePerPage, totalBundles(response))));
        }

        ListBundlesResponse response = planService.listAllBundlesPaginated(p + 1, effectivePerPage, effectiveDirection, effectiveOrderBy, search);
        return ResponseEntity.ok(ApiResponse.success(response, "Success", PaginationMeta.simple(p, effectivePerPage, totalBundles(response))));
    }
    
    @GetMapping("/regions")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableRegions() {
        List<String> regions = planService.getAvailableRegions();
        return ResponseEntity.ok(ApiResponse.success(regions));
    }

    private long totalBundles(ListBundlesResponse response) {
        if (response == null || response.getBundles() == null) return 0;
        return response.getBundles().size();
    }

    private SortParts parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return new SortParts("name", "asc");
        }
        String[] parts = sort.split(",", 2);
        String field = parts[0].trim();
        String dir = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";
        if (!"desc".equals(dir)) dir = "asc";
        if (field.isBlank()) field = "name";
        return new SortParts(field, dir);
    }

    private static final class SortParts {
        private final String field;
        private final String direction;

        private SortParts(String field, String direction) {
            this.field = field;
            this.direction = direction;
        }
    }
}
