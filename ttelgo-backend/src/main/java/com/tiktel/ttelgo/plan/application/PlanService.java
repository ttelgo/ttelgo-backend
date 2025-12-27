package com.tiktel.ttelgo.plan.application;

import com.tiktel.ttelgo.integration.esimgo.dto.BundleResponse;
import com.tiktel.ttelgo.plan.application.port.EsimGoPort;
import com.tiktel.ttelgo.plan.api.dto.ListBundlesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlanService {
    
    private final EsimGoPort esimGoPort;
    
    // In-memory cache for all bundles (TTL: 1 hour)
    private static final long CACHE_TTL_MS = 60 * 60 * 1000; // 1 hour
    private volatile CachedBundles cachedAllBundles = null;
    
    @Autowired
    public PlanService(EsimGoPort esimGoPort) {
        this.esimGoPort = esimGoPort;
    }
    
    /**
     * Simple cache wrapper for bundles
     */
    private static class CachedBundles {
        final ListBundlesResponse bundles;
        final long timestamp;
        
        CachedBundles(ListBundlesResponse bundles) {
            this.bundles = bundles;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
    
    /**
     * List all bundles - loads ALL bundles using pagination
     */
    public ListBundlesResponse listAllBundles() {
        try {
            ListBundlesResponse result = loadAllBundles();
            if (result == null) {
                log.warn("loadAllBundles returned null");
                result = new ListBundlesResponse();
                result.setBundles(new java.util.ArrayList<>());
            }
            if (result.getBundles() == null) {
                result.setBundles(new java.util.ArrayList<>());
            }
            return result;
        } catch (Exception e) {
            log.error("Error in listAllBundles: {}", e.getMessage(), e);
            ListBundlesResponse result = new ListBundlesResponse();
            result.setBundles(new java.util.ArrayList<>());
            return result;
        }
    }
    
    /**
     * List all bundles with pagination and search
     */
    public ListBundlesResponse listAllBundlesPaginated(Integer page, Integer perPage, String direction, String orderBy, String search) {
        BundleResponse response = esimGoPort.listBundles(page, perPage, direction, orderBy, search);
        return mapToResponse(response);
    }
    
    /**
     * List bundles by country with pagination
     */
    public ListBundlesResponse listBundlesByCountryPaginated(String countryIso, Integer page, Integer perPage, String direction, String orderBy) {
        // For country-specific, we still use the country filter but add pagination
        BundleResponse response = esimGoPort.listBundlesByCountry(countryIso);
        // Note: eSIMGo API may not support pagination with country filter simultaneously
        // If needed, we can implement client-side pagination here
        return mapToResponse(response);
    }
    
    /**
     * Load all plans by making paginated requests until all are loaded
     * This is used for admin panel to load all plans once for client-side filtering
     */
    public ListBundlesResponse loadAllPlans() {
        ListBundlesResponse allPlans = new ListBundlesResponse();
        allPlans.setBundles(new java.util.ArrayList<>());
        
        int page = 1;
        int perPage = 100; // Load 100 per page to reduce API calls
        boolean hasMore = true;
        
        while (hasMore) {
            BundleResponse response = esimGoPort.listBundles(page, perPage, "asc", null, null);
            if (response != null && response.getBundles() != null && !response.getBundles().isEmpty()) {
                ListBundlesResponse pageResponse = mapToResponse(response);
                if (pageResponse.getBundles() != null) {
                    allPlans.getBundles().addAll(pageResponse.getBundles());
                }
                
                // If we got less than perPage, we've reached the end
                hasMore = response.getBundles().size() == perPage;
                page++;
            } else {
                hasMore = false;
            }
        }
        
        return allPlans;
    }
    
    public ListBundlesResponse listBundlesByCountry(String countryIso) {
        BundleResponse response = esimGoPort.listBundlesByCountry(countryIso);
        return mapToResponse(response);
    }
    
    public ListBundlesResponse.BundleDto getBundleDetails(String bundleName) {
        BundleResponse.Bundle bundle = esimGoPort.getBundleDetails(bundleName);
        return mapBundleToDto(bundle);
    }
    
    /**
     * List local eSIM bundles (single country bundles)
     * Loads ALL bundles using pagination for complete data
     */
    public ListBundlesResponse listLocalBundles() {
        log.info("=== listLocalBundles() called ===");
        try {
            ListBundlesResponse allBundles = loadAllBundles();
            log.info("loadAllBundles returned {} bundles", 
                    allBundles != null && allBundles.getBundles() != null ? allBundles.getBundles().size() : 0);
            
            ListBundlesResponse result = new ListBundlesResponse();
            result.setBundles(new java.util.ArrayList<>());
            
            if (allBundles == null || allBundles.getBundles() == null) {
                log.warn("loadAllBundles returned null or empty bundles");
                return result;
            }
            
            result.setBundles(allBundles.getBundles().stream()
                    .filter(bundle -> bundle != null && bundle.getCountries() != null && bundle.getCountries().size() == 1)
                    .collect(Collectors.toList()));
            log.info("Filtered to {} local bundles", result.getBundles().size());
            return result;
        } catch (Exception e) {
            log.error("=== ERROR in listLocalBundles() ===");
            log.error("Error: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            log.error("Stack trace:", e);
            ListBundlesResponse result = new ListBundlesResponse();
            result.setBundles(new java.util.ArrayList<>());
            return result;
        }
    }
    
    /**
     * Load all bundles from eSIMGo API using pagination
     * This method fetches all available bundles by making paginated requests
     * Uses in-memory cache to avoid repeated API calls (1 hour TTL)
     */
    private ListBundlesResponse loadAllBundles() {
        log.info("=== loadAllBundles() called ===");
        // Check cache first
        if (cachedAllBundles != null && !cachedAllBundles.isExpired()) {
            int bundleCount = cachedAllBundles.bundles.getBundles() != null ? cachedAllBundles.bundles.getBundles().size() : 0;
            log.info("Returning cached bundles ({} bundles)", bundleCount);
            return cachedAllBundles.bundles;
        }
        
        log.info("Loading all bundles from eSIMGo API (cache miss or expired)");
        ListBundlesResponse allBundles = new ListBundlesResponse();
        allBundles.setBundles(new java.util.ArrayList<>());
        
        int page = 1;
        int perPage = 500; // Load 500 per page to significantly reduce API calls (was 100)
        boolean hasMore = true;
        int totalLoaded = 0;
        int maxPages = 1000; // Safety limit to prevent infinite loops
        int pageCount = 0;
        
        try {
            while (hasMore && pageCount < maxPages) {
                pageCount++;
                BundleResponse response = null;
                try {
                    response = esimGoPort.listBundles(page, perPage, "asc", null, null);
                } catch (Exception e) {
                    log.error("Exception calling eSIMGo API at page {}: {}", page, e.getMessage(), e);
                    // If we have cached data, return that instead of failing
                    if (cachedAllBundles != null) {
                        log.warn("Returning stale cached data due to API exception");
                        return cachedAllBundles.bundles;
                    }
                    // Otherwise, break and return what we have so far
                    hasMore = false;
                    break;
                }
                
                if (response == null) {
                    log.warn("Received null response from eSIMGo API at page {}", page);
                    // If we have cached data, return that instead
                    if (cachedAllBundles != null && totalLoaded == 0) {
                        log.warn("Returning stale cached data due to null response");
                        return cachedAllBundles.bundles;
                    }
                    hasMore = false;
                    break;
                }
                
                if (response.getBundles() == null || response.getBundles().isEmpty()) {
                    log.debug("No more bundles at page {}", page);
                    hasMore = false;
                    break;
                }
                
                try {
                    ListBundlesResponse pageResponse = mapToResponse(response);
                    if (pageResponse != null && pageResponse.getBundles() != null) {
                        allBundles.getBundles().addAll(pageResponse.getBundles());
                        totalLoaded += pageResponse.getBundles().size();
                        log.debug("Loaded {} bundles from page {} (total so far: {})", 
                                pageResponse.getBundles().size(), page, totalLoaded);
                    }
                    
                    // If we got less than perPage, we've reached the end
                    hasMore = response.getBundles().size() == perPage;
                    page++;
                } catch (Exception e) {
                    log.error("Error mapping response at page {}: {}", page, e.getMessage(), e);
                    // Continue to next page instead of failing completely
                    hasMore = response.getBundles().size() == perPage;
                    page++;
                }
            }
            
            if (pageCount >= maxPages) {
                log.warn("Reached maximum page limit ({}), stopping pagination", maxPages);
            }
            
            log.info("Loaded {} total bundles from eSIMGo API across {} pages", totalLoaded, pageCount);
            
            // Only cache if we got some bundles
            if (totalLoaded > 0) {
                cachedAllBundles = new CachedBundles(allBundles);
            } else {
                log.warn("No bundles loaded, not caching empty result");
            }
            
        } catch (Exception e) {
            log.error("=== CRITICAL ERROR in loadAllBundles() ===");
            log.error("Error message: {}", e.getMessage());
            log.error("Error class: {}", e.getClass().getName());
            log.error("Full stack trace:", e);
            if (e.getCause() != null) {
                log.error("Root cause: {}", e.getCause().getMessage());
                log.error("Root cause class: {}", e.getCause().getClass().getName());
                if (e.getCause().getCause() != null) {
                    log.error("Root cause of root cause: {}", e.getCause().getCause().getMessage());
                }
            }
            // Return empty result instead of throwing to prevent 500 errors
            // If we have cached data, return that instead
            if (cachedAllBundles != null) {
                log.warn("Returning stale cached data due to API error");
                return cachedAllBundles.bundles;
            }
            // Return empty result
            log.warn("Returning empty bundles list due to error");
            return allBundles;
        }
        
        return allBundles;
    }
    
    /**
     * Clear the bundle cache (useful for testing or manual refresh)
     */
    public void clearBundleCache() {
        log.info("Clearing bundle cache");
        cachedAllBundles = null;
    }
    
    /**
     * List regional eSIM bundles
     * Strategy:
     * 1. First try to get bundles with group containing "regional" or "region"
     * 2. Then filter all bundles for 2-49 countries OR group containing regional keywords
     * 3. If no true regional bundles found, return all bundles (frontend will group by region)
     * Loads ALL bundles using pagination for complete data
     */
    public ListBundlesResponse listRegionalBundles() {
        try {
            ListBundlesResponse allBundles = loadAllBundles();
            ListBundlesResponse result = new ListBundlesResponse();
            result.setBundles(new java.util.ArrayList<>());
            
            if (allBundles == null || allBundles.getBundles() == null) {
                log.warn("loadAllBundles returned null or empty bundles");
                return result;
            }
            
            result.setBundles(allBundles.getBundles().stream()
                    .filter(bundle -> bundle != null && bundle.getCountries() != null)
                    .filter(bundle -> {
                        int countryCount = bundle.getCountries().size();
                        // Regional: 2-49 countries OR group contains "regional"
                        boolean isRegionalByCount = countryCount >= 2 && countryCount < 50;
                        boolean isRegionalByGroup = bundle.getGroup() != null && 
                                bundle.getGroup().stream()
                                        .anyMatch(group -> group != null && 
                                                (group.toLowerCase().contains("regional") ||
                                                 group.toLowerCase().contains("region")));
                        return isRegionalByCount || isRegionalByGroup;
                    })
                    .collect(Collectors.toList()));
            
            // If no true regional bundles found, return all bundles
            // Frontend will group them by region to create regional views
            if (result.getBundles().isEmpty()) {
                result.setBundles(allBundles.getBundles());
            }
            return result;
        } catch (Exception e) {
            log.error("Error in listRegionalBundles: {}", e.getMessage(), e);
            ListBundlesResponse result = new ListBundlesResponse();
            result.setBundles(new java.util.ArrayList<>());
            return result;
        }
    }
    
    /**
     * List regional eSIM bundles filtered by region name
     * @param regionName The region name (e.g., "Europe", "Asia", "North America", etc.)
     * Loads ALL bundles using pagination for complete data
     */
    public ListBundlesResponse listRegionalBundlesByRegion(String regionName) {
        ListBundlesResponse allBundles = loadAllBundles();
        ListBundlesResponse result = new ListBundlesResponse();
        result.setBundles(allBundles.getBundles().stream()
                .filter(bundle -> {
                    if (bundle.getCountries() == null || bundle.getCountries().isEmpty()) return false;
                    
                    int countryCount = bundle.getCountries().size();
                    // Regional: 2-49 countries
                    boolean isRegionalByCount = countryCount >= 2 && countryCount < 50;
                    
                    // Check if bundle belongs to the specified region
                    // A bundle belongs to a region if:
                    // 1. All countries in the bundle are from that region, OR
                    // 2. The majority of countries are from that region
                    boolean belongsToRegion = bundle.getCountries().stream()
                            .anyMatch(country -> {
                                if (country == null || country.getRegion() == null) return false;
                                // Normalize region names for comparison
                                String bundleRegion = normalizeRegionName(country.getRegion());
                                String targetRegion = normalizeRegionName(regionName);
                                return bundleRegion.equalsIgnoreCase(targetRegion);
                            });
                    
                    return isRegionalByCount && belongsToRegion;
                })
                .collect(Collectors.toList()));
        return result;
    }
    
    /**
     * Get list of available regions from regional bundles
     * Loads ALL bundles using pagination for complete data
     */
    public List<String> getAvailableRegions() {
        ListBundlesResponse allBundles = loadAllBundles();
        return allBundles.getBundles().stream()
                .filter(bundle -> {
                    if (bundle.getCountries() == null) return false;
                    int countryCount = bundle.getCountries().size();
                    // Regional: 2-49 countries
                    return countryCount >= 2 && countryCount < 50;
                })
                .flatMap(bundle -> bundle.getCountries().stream()
                        .map(country -> country != null ? country.getRegion() : null)
                        .filter(region -> region != null && !region.trim().isEmpty()))
                .map(this::normalizeRegionName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
    
    /**
     * Normalize region names to standard format
     */
    private String normalizeRegionName(String region) {
        if (region == null) return "";
        String normalized = region.trim();
        // Map common variations to standard names
        if (normalized.equalsIgnoreCase("Middle East")) return "Middle East";
        if (normalized.equalsIgnoreCase("North America")) return "North America";
        if (normalized.equalsIgnoreCase("South America")) return "South America";
        if (normalized.equalsIgnoreCase("Oceania")) return "Oceania";
        if (normalized.equalsIgnoreCase("Africa")) return "Africa";
        if (normalized.equalsIgnoreCase("Asia")) return "Asia";
        if (normalized.equalsIgnoreCase("Europe")) return "Europe";
        return normalized;
    }
    
    /**
     * List global eSIM bundles
     * According to eSIMGo API response structure, global bundles are identified by:
     * 1. Country name contains "Global" (e.g., "Global - Light", "Global")
     * 2. Country name contains "Europe + USA", "Asia", "Oceania", "North America" (regional/global)
     * 3. 80+ countries in countries array
     * 4. roamingEnabled is an array with 10+ countries (indicates global roaming)
     * 5. Group contains "Global", "Long Duration" (for long-term global plans)
     * Loads ALL bundles using pagination for complete data
     */
    public ListBundlesResponse listGlobalBundles() {
        try {
            ListBundlesResponse allBundles = loadAllBundles();
            ListBundlesResponse result = new ListBundlesResponse();
            result.setBundles(new java.util.ArrayList<>());
            
            if (allBundles == null || allBundles.getBundles() == null) {
                log.warn("loadAllBundles returned null or empty bundles");
                return result;
            }
            
            result.setBundles(allBundles.getBundles().stream()
                    .filter(bundle -> bundle != null && bundle.getCountries() != null && !bundle.getCountries().isEmpty())
                    .filter(bundle -> {
                        // Check country name for global indicators
                        String firstCountryName = bundle.getCountries().get(0).getName();
                        String firstCountryIso = bundle.getCountries().get(0).getIso();
                    
                    // Global indicators in country name/ISO
                    boolean isGlobalByName = firstCountryName != null && (
                        firstCountryName.contains("Global") ||
                        firstCountryName.contains("Europe + USA") ||
                        firstCountryName.equalsIgnoreCase("Asia") ||
                        firstCountryName.equalsIgnoreCase("Oceania") ||
                        firstCountryName.equalsIgnoreCase("North America") ||
                        firstCountryName.equalsIgnoreCase("Middle East") ||
                        firstCountryName.equalsIgnoreCase("CENAM") ||
                        firstCountryName.equalsIgnoreCase("CIS") ||
                        firstCountryName.equalsIgnoreCase("Europe Lite") ||
                        firstCountryName.equalsIgnoreCase("Europe+")
                    );
                    
                    boolean isGlobalByIso = firstCountryIso != null && (
                        firstCountryIso.equalsIgnoreCase("Global - Light") ||
                        firstCountryIso.equalsIgnoreCase("Global") ||
                        firstCountryIso.equalsIgnoreCase("Europe + USA") ||
                        firstCountryIso.equalsIgnoreCase("Asia") ||
                        firstCountryIso.equalsIgnoreCase("Oceania")
                    );
                    
                    int countryCount = bundle.getCountries().size();
                    // Global: 80+ countries
                    boolean isGlobalByCount = countryCount >= 80;
                    
                    // OR roaming enabled (indicates multi-country/global coverage)
                    // The deserializer now returns true if roamingEnabled is array with 10+ countries
                    boolean isGlobalByRoaming = Boolean.TRUE.equals(bundle.getRoamingEnabled());
                    
                    // OR group contains "global" or "Long Duration" (for long-term global plans)
                    boolean isGlobalByGroup = false;
                    if (bundle.getGroup() != null && !bundle.getGroup().isEmpty()) {
                        isGlobalByGroup = bundle.getGroup().stream()
                                .anyMatch(group -> {
                                    if (group == null) return false;
                                    String groupLower = group.toLowerCase();
                                    return groupLower.contains("global") ||
                                           groupLower.contains("long duration") ||
                                           groupLower.contains("worldwide") ||
                                           groupLower.contains("world");
                                });
                    }
                    
                    boolean isGlobal = isGlobalByName || isGlobalByIso || isGlobalByCount || 
                                     isGlobalByRoaming || isGlobalByGroup;
                    
                    return isGlobal;
                })
                .collect(Collectors.toList()));
            return result;
        } catch (Exception e) {
            log.error("Error in listGlobalBundles: {}", e.getMessage(), e);
            ListBundlesResponse result = new ListBundlesResponse();
            result.setBundles(new java.util.ArrayList<>());
            return result;
        }
    }
    
    private ListBundlesResponse mapToResponse(BundleResponse response) {
        ListBundlesResponse result = new ListBundlesResponse();
        result.setBundles(new java.util.ArrayList<>());
        
        if (response == null) {
            log.warn("Received null BundleResponse");
            return result;
        }
        
        if (response.getBundles() == null || response.getBundles().isEmpty()) {
            log.debug("BundleResponse has no bundles");
            return result;
        }
        
        try {
            result.setBundles(response.getBundles().stream()
                    .filter(bundle -> bundle != null) // Filter out null bundles
                    .map(bundle -> {
                        try {
                            return mapBundleToDto(bundle);
                        } catch (Exception e) {
                            log.warn("Error mapping bundle {}: {}", bundle != null ? bundle.getName() : "null", e.getMessage());
                            return null; // Return null for failed mappings
                        }
                    })
                    .filter(dto -> dto != null) // Filter out null DTOs
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Error mapping BundleResponse to ListBundlesResponse: {}", e.getMessage(), e);
            // Return empty result instead of throwing
        }
        
        return result;
    }
    
    private ListBundlesResponse.BundleDto mapBundleToDto(BundleResponse.Bundle bundle) {
        if (bundle == null) {
            log.warn("Attempted to map null bundle");
            return null;
        }
        
        try {
            ListBundlesResponse.BundleDto dto = new ListBundlesResponse.BundleDto();
            dto.setName(bundle.getName());
            dto.setDescription(bundle.getDescription());
            dto.setDataAmount(bundle.getDataAmount());
            dto.setDuration(bundle.getDuration());
            dto.setAutostart(bundle.getAutostart() != null ? bundle.getAutostart() : false);
            dto.setUnlimited(bundle.getUnlimited() != null ? bundle.getUnlimited() : false);
            dto.setRoamingEnabled(bundle.getRoamingEnabled());
            dto.setImageUrl(bundle.getImageUrl());
            dto.setPrice(bundle.getPrice());
            dto.setGroup(bundle.getGroup());
            dto.setBillingType(bundle.getBillingType());
            dto.setPotentialSpeeds(bundle.getPotentialSpeeds());
            
            if (bundle.getCountries() != null && !bundle.getCountries().isEmpty()) {
                dto.setCountries(bundle.getCountries().stream()
                        .filter(country -> country != null) // Filter null countries
                        .map(country -> {
                            try {
                                ListBundlesResponse.CountryDto countryDto = new ListBundlesResponse.CountryDto();
                                countryDto.setName(country.getName());
                                countryDto.setRegion(country.getRegion());
                                countryDto.setIso(country.getIso());
                                return countryDto;
                            } catch (Exception e) {
                                log.warn("Error mapping country: {}", e.getMessage());
                                return null;
                            }
                        })
                        .filter(countryDto -> countryDto != null) // Filter null country DTOs
                        .collect(Collectors.toList()));
            } else {
                dto.setCountries(new java.util.ArrayList<>());
            }
            
            return dto;
        } catch (Exception e) {
            log.error("Error mapping bundle to DTO: {}", e.getMessage(), e);
            return null;
        }
    }
}
