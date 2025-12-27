package com.tiktel.ttelgo.plan.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.dto.PaginationMeta;
import com.tiktel.ttelgo.plan.api.dto.ListBundlesResponse;
import com.tiktel.ttelgo.plan.application.PlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/bundles")
public class PlanController {
    
    private final PlanService planService;
    
    @Autowired
    public PlanController(PlanService planService) {
        this.planService = planService;
    }
    
    /**
     * List all available bundles
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ListBundlesResponse>> listBundles(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer size,
            @RequestParam(required = false, defaultValue = "name,asc") String sort,
            // legacy / external paging params (still accepted)
            @RequestParam(required = false) Integer perPage,
            @RequestParam(required = false, defaultValue = "asc") String direction,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String countryIso,
            @RequestParam(required = false) String type
    ) {
        log.info("=== BUNDLES API CALL START ===");
        log.info("Request params - page: {}, size: {}, type: {}, countryIso: {}, search: {}", 
                page, size, type, countryIso, search);
        try {
            // RESTful filtering via query params:
            // - type=local|regional|global
            // - countryIso=GB
            // - page/perPage/direction/orderBy/search
            ListBundlesResponse response;

            int p = page != null ? page : 0;
            int s = size != null ? size : 50;

            if (type != null && !type.trim().isEmpty()) {
                try {
                    String t = type.trim().toLowerCase();
                    response = switch (t) {
                        case "local" -> planService.listLocalBundles();
                        case "regional" -> planService.listRegionalBundles();
                        case "global" -> planService.listGlobalBundles();
                        default -> planService.listAllBundles();
                    };
                    
                    if (response == null) {
                        response = new ListBundlesResponse();
                        response.setBundles(new java.util.ArrayList<>());
                    }
                    
                    // If size is explicitly set to a small value, paginate. Otherwise return all bundles
                    // This allows frontend to get all bundles by not specifying size or using a large size
                    if (s > 0 && s < 1000) {
                        ListBundlesResponse paged = paginateInMemory(response, p, s, sort);
                        return ResponseEntity.ok(ApiResponse.success(paged, "Success", PaginationMeta.simple(p, s, totalBundles(response))));
                    } else {
                        // Return all bundles (apply sorting only)
                        ListBundlesResponse sorted = applySortOnly(response, sort);
                        long total = totalBundles(sorted);
                        return ResponseEntity.ok(ApiResponse.success(sorted, "Success", PaginationMeta.simple(0, (int)total, total)));
                    }
                } catch (Exception e) {
                    log.error("Error loading bundles by type {}: {}", type, e.getMessage(), e);
                    // Return empty response instead of 500
                    response = new ListBundlesResponse();
                    response.setBundles(new java.util.ArrayList<>());
                    return ResponseEntity.ok(ApiResponse.success(response, "No bundles available", PaginationMeta.simple(0, 0, 0)));
                }
            }

            if (countryIso != null && !countryIso.trim().isEmpty()) {
                try {
                    response = planService.listBundlesByCountry(countryIso.trim());
                    if (response == null) {
                        response = new ListBundlesResponse();
                        response.setBundles(new java.util.ArrayList<>());
                    }
                    // If size is explicitly set to a small value, paginate. Otherwise return all bundles
                    if (s > 0 && s < 1000) {
                        ListBundlesResponse paged = paginateInMemory(response, p, s, sort);
                        return ResponseEntity.ok(ApiResponse.success(paged, "Success", PaginationMeta.simple(p, s, totalBundles(response))));
                    } else {
                        // Return all bundles (apply sorting only)
                        ListBundlesResponse sorted = applySortOnly(response, sort);
                        long total = totalBundles(sorted);
                        return ResponseEntity.ok(ApiResponse.success(sorted, "Success", PaginationMeta.simple(0, (int)total, total)));
                    }
                } catch (Exception e) {
                    log.error("Error loading bundles by country {}: {}", countryIso, e.getMessage(), e);
                    response = new ListBundlesResponse();
                    response.setBundles(new java.util.ArrayList<>());
                    return ResponseEntity.ok(ApiResponse.success(response, "No bundles available", PaginationMeta.simple(0, 0, 0)));
                }
            }

            // Prefer Spring-style pagination params: page (0-based) + size
            // The EsimGo pagination is 1-based page, so we adapt.
            if (page != null || size != null || orderBy != null || search != null || perPage != null) {
                try {
                    int effectivePerPage = perPage != null ? perPage : (size != null ? size : 50);
                    SortParts sortParts = parseSort(sort);
                    String effectiveOrderBy = orderBy != null ? orderBy : sortParts.field;
                    String effectiveDirection = direction != null ? direction : sortParts.direction;

                    response = planService.listAllBundlesPaginated(
                            p + 1,
                            effectivePerPage,
                            effectiveDirection,
                            effectiveOrderBy,
                            search
                    );
                    if (response == null) {
                        response = new ListBundlesResponse();
                        response.setBundles(new java.util.ArrayList<>());
                    }
                    // We don't know total count from upstream; expose current page size as totalElements for consistency.
                    return ResponseEntity.ok(ApiResponse.success(response, "Success", PaginationMeta.simple(p, effectivePerPage, totalBundles(response))));
                } catch (Exception e) {
                    log.error("Error loading paginated bundles: {}", e.getMessage(), e);
                    response = new ListBundlesResponse();
                    response.setBundles(new java.util.ArrayList<>());
                    return ResponseEntity.ok(ApiResponse.success(response, "No bundles available", PaginationMeta.simple(0, 0, 0)));
                }
            }

            try {
                response = planService.listAllBundles();
                
                if (response == null) {
                    response = new ListBundlesResponse();
                    response.setBundles(new java.util.ArrayList<>());
                }
                
                // If size is explicitly set to a small value, paginate. Otherwise return all bundles
                if (s > 0 && s < 1000) {
                    ListBundlesResponse paged = paginateInMemory(response, p, s, sort);
                    return ResponseEntity.ok(ApiResponse.success(paged, "Success", PaginationMeta.simple(p, s, totalBundles(response))));
                } else {
                    // Return all bundles (apply sorting only)
                    ListBundlesResponse sorted = applySortOnly(response, sort);
                    long total = totalBundles(sorted);
                    return ResponseEntity.ok(ApiResponse.success(sorted, "Success", PaginationMeta.simple(0, (int)total, total)));
                }
            } catch (Exception e) {
                log.error("Error loading all bundles: {}", e.getMessage(), e);
                // Return empty response instead of 500
                response = new ListBundlesResponse();
                response.setBundles(new java.util.ArrayList<>());
                return ResponseEntity.ok(ApiResponse.success(response, "No bundles available", PaginationMeta.simple(0, 0, 0)));
            }
        } catch (Exception e) {
            log.error("=== CRITICAL ERROR in listBundles endpoint ===");
            log.error("Error message: {}", e.getMessage());
            log.error("Error class: {}", e.getClass().getName());
            log.error("Stack trace:", e);
            if (e.getCause() != null) {
                log.error("Root cause: {}", e.getCause().getMessage());
                log.error("Root cause class: {}", e.getCause().getClass().getName());
            }
            // Return empty response with 200 status instead of 500
            ListBundlesResponse emptyResponse = new ListBundlesResponse();
            emptyResponse.setBundles(new java.util.ArrayList<>());
            log.info("=== Returning empty response to prevent 500 error ===");
            return ResponseEntity.ok(ApiResponse.success(emptyResponse, "No bundles available", PaginationMeta.simple(0, 0, 0)));
        }
    }
    
    /**
     * Get bundle details by bundle name
     */
    @GetMapping("/{bundleName}")
    public ResponseEntity<ApiResponse<ListBundlesResponse.BundleDto>> getBundleDetails(
            @PathVariable String bundleName) {
        ListBundlesResponse.BundleDto bundle = planService.getBundleDetails(bundleName);
        return ResponseEntity.ok(ApiResponse.success(bundle));
    }

    private long totalBundles(ListBundlesResponse response) {
        if (response == null || response.getBundles() == null) return 0;
        return response.getBundles().size();
    }

    private ListBundlesResponse paginateInMemory(ListBundlesResponse response, int page, int size, String sort) {
        if (response == null || response.getBundles() == null) return response;
        List<ListBundlesResponse.BundleDto> bundles = response.getBundles();
        List<ListBundlesResponse.BundleDto> sorted = applySort(bundles, sort);
        List<ListBundlesResponse.BundleDto> paged = slice(sorted, page, size);
        ListBundlesResponse out = new ListBundlesResponse();
        out.setBundles(paged);
        return out;
    }

    private List<ListBundlesResponse.BundleDto> slice(List<ListBundlesResponse.BundleDto> items, int page, int size) {
        if (items == null || items.isEmpty()) return items;
        if (size <= 0) return items;
        int from = Math.max(0, page) * size;
        if (from >= items.size()) return List.of();
        int to = Math.min(items.size(), from + size);
        return items.subList(from, to);
    }

    private List<ListBundlesResponse.BundleDto> applySort(List<ListBundlesResponse.BundleDto> items, String sort) {
        if (items == null) return List.of();
        SortParts parts = parseSort(sort);

        Comparator<ListBundlesResponse.BundleDto> comparator = Comparator.comparing(
                b -> b.getName() != null ? b.getName() : "",
                String.CASE_INSENSITIVE_ORDER
        );

        if ("price".equals(parts.field)) {
            comparator = Comparator.comparing(ListBundlesResponse.BundleDto::getPrice, Comparator.nullsLast(Double::compareTo));
        } else if ("duration".equals(parts.field)) {
            comparator = Comparator.comparing(ListBundlesResponse.BundleDto::getDuration, Comparator.nullsLast(Integer::compareTo));
        } else if ("dataAmount".equals(parts.field)) {
            comparator = Comparator.comparing(ListBundlesResponse.BundleDto::getDataAmount, Comparator.nullsLast(Integer::compareTo));
        }

        if ("desc".equals(parts.direction)) comparator = comparator.reversed();

        return items.stream().sorted(comparator).collect(Collectors.toList());
    }
    
    /**
     * Apply sorting only (no pagination) - used when returning all bundles
     */
    private ListBundlesResponse applySortOnly(ListBundlesResponse response, String sort) {
        if (response == null || response.getBundles() == null) return response;
        List<ListBundlesResponse.BundleDto> sorted = applySort(response.getBundles(), sort);
        ListBundlesResponse out = new ListBundlesResponse();
        out.setBundles(sorted);
        return out;
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
