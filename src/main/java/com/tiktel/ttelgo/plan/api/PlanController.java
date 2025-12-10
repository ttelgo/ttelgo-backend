package com.tiktel.ttelgo.plan.api;

import com.tiktel.ttelgo.plan.api.dto.ListBundlesResponse;
import com.tiktel.ttelgo.plan.application.PlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plans")
public class PlanController {
    
    private final PlanService planService;
    
    @Autowired
    public PlanController(PlanService planService) {
        this.planService = planService;
    }
    
    /**
     * List all available bundles
     */
    @GetMapping("/bundles")
    public ResponseEntity<ListBundlesResponse> listAllBundles() {
        ListBundlesResponse response = planService.listAllBundles();
        return ResponseEntity.ok(response);
    }
    
    /**
     * List bundles by country ISO code (e.g., GB, US, AD)
     */
    @GetMapping("/bundles/country")
    public ResponseEntity<ListBundlesResponse> listBundlesByCountry(
            @RequestParam String countryIso) {
        ListBundlesResponse response = planService.listBundlesByCountry(countryIso);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get bundle details by bundle name
     */
    @GetMapping("/bundles/{bundleName}")
    public ResponseEntity<ListBundlesResponse.BundleDto> getBundleDetails(
            @PathVariable String bundleName) {
        ListBundlesResponse.BundleDto bundle = planService.getBundleDetails(bundleName);
        return ResponseEntity.ok(bundle);
    }
    
    /**
     * List local eSIM bundles (single country)
     */
    @GetMapping("/bundles/local")
    public ResponseEntity<ListBundlesResponse> listLocalBundles() {
        ListBundlesResponse response = planService.listLocalBundles();
        return ResponseEntity.ok(response);
    }
    
    /**
     * List regional eSIM bundles (multiple countries in a region)
     */
    @GetMapping("/bundles/regional")
    public ResponseEntity<ListBundlesResponse> listRegionalBundles() {
        ListBundlesResponse response = planService.listRegionalBundles();
        return ResponseEntity.ok(response);
    }
    
    /**
     * List global eSIM bundles (many countries globally)
     */
    @GetMapping("/bundles/global")
    public ResponseEntity<ListBundlesResponse> listGlobalBundles() {
        ListBundlesResponse response = planService.listGlobalBundles();
        return ResponseEntity.ok(response);
    }
}
