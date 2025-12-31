package com.tiktel.ttelgo.catalogue.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.integration.esimgo.EsimGoService;
import com.tiktel.ttelgo.integration.esimgo.domain.Bundle;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Catalogue API for browsing available eSIM bundles
 * Public endpoint (no authentication required)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/catalogue")
@Tag(name = "Catalogue", description = "Browse eSIM bundles")
public class CatalogueController {
    
    private final EsimGoService esimGoService;
    
    public CatalogueController(EsimGoService esimGoService) {
        this.esimGoService = esimGoService;
    }
    
    @Operation(summary = "Get all bundles", description = "Get all available eSIM bundles")
    @GetMapping
    public ApiResponse<List<Bundle>> getAllBundles() {
        log.info("Getting all bundles");
        List<Bundle> bundles = esimGoService.getBundles();
        return ApiResponse.success(bundles);
    }
    
    @Operation(summary = "Get bundles by country", description = "Get bundles for a specific country")
    @GetMapping("/countries/{countryIso}")
    public ApiResponse<List<Bundle>> getBundlesByCountry(@PathVariable String countryIso) {
        log.info("Getting bundles for country: {}", countryIso);
        List<Bundle> bundles = esimGoService.getBundlesByCountry(countryIso.toUpperCase());
        return ApiResponse.success(bundles);
    }
    
    @Operation(summary = "Get bundle details", description = "Get details of a specific bundle")
    @GetMapping("/bundles/{bundleCode}")
    public ApiResponse<Bundle> getBundleDetails(@PathVariable String bundleCode) {
        log.info("Getting bundle details: {}", bundleCode);
        Bundle bundle = esimGoService.getBundleDetails(bundleCode);
        return ApiResponse.success(bundle);
    }
}

