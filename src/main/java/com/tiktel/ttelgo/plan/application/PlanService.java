package com.tiktel.ttelgo.plan.application;

import com.tiktel.ttelgo.integration.esimgo.EsimGoClient;
import com.tiktel.ttelgo.integration.esimgo.dto.BundleResponse;
import com.tiktel.ttelgo.plan.api.dto.ListBundlesResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlanService {
    
    private final EsimGoClient esimGoClient;
    
    @Autowired
    public PlanService(EsimGoClient esimGoClient) {
        this.esimGoClient = esimGoClient;
    }
    
    public ListBundlesResponse listAllBundles() {
        BundleResponse response = esimGoClient.listBundles();
        return mapToResponse(response);
    }
    
    /**
     * List all bundles with pagination and search
     */
    public ListBundlesResponse listAllBundlesPaginated(Integer page, Integer perPage, String direction, String orderBy, String search) {
        BundleResponse response = esimGoClient.listBundles(page, perPage, direction, orderBy, search);
        return mapToResponse(response);
    }
    
    /**
     * List bundles by country with pagination
     */
    public ListBundlesResponse listBundlesByCountryPaginated(String countryIso, Integer page, Integer perPage, String direction, String orderBy) {
        // For country-specific, we still use the country filter but add pagination
        BundleResponse response = esimGoClient.listBundlesByCountry(countryIso);
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
            BundleResponse response = esimGoClient.listBundles(page, perPage, "asc", null, null);
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
        BundleResponse response = esimGoClient.listBundlesByCountry(countryIso);
        return mapToResponse(response);
    }
    
    public ListBundlesResponse.BundleDto getBundleDetails(String bundleName) {
        BundleResponse.Bundle bundle = esimGoClient.getBundleDetails(bundleName);
        return mapBundleToDto(bundle);
    }
    
    /**
     * List local eSIM bundles (single country bundles)
     */
    public ListBundlesResponse listLocalBundles() {
        BundleResponse response = esimGoClient.listBundles();
        ListBundlesResponse result = new ListBundlesResponse();
        if (response != null && response.getBundles() != null) {
            result.setBundles(response.getBundles().stream()
                    .filter(bundle -> bundle.getCountries() != null && bundle.getCountries().size() == 1)
                    .map(this::mapBundleToDto)
                    .collect(Collectors.toList()));
        }
        return result;
    }
    
    /**
     * List regional eSIM bundles
     * Strategy:
     * 1. First try to get bundles with group containing "regional" or "region"
     * 2. Then filter all bundles for 2-49 countries OR group containing regional keywords
     * 3. If no true regional bundles found, return all bundles (frontend will group by region)
     */
    public ListBundlesResponse listRegionalBundles() {
        // Try to get bundles by group first (if eSIMGo has regional groups)
        BundleResponse response = esimGoClient.listBundles();
        ListBundlesResponse result = new ListBundlesResponse();
        if (response != null && response.getBundles() != null) {
            result.setBundles(response.getBundles().stream()
                    .filter(bundle -> {
                        if (bundle.getCountries() == null) return false;
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
                    .map(this::mapBundleToDto)
                    .collect(Collectors.toList()));
            
            // If no true regional bundles found, return all bundles
            // Frontend will group them by region to create regional views
            if (result.getBundles().isEmpty()) {
                result.setBundles(response.getBundles().stream()
                        .map(this::mapBundleToDto)
                        .collect(Collectors.toList()));
            }
        }
        return result;
    }
    
    /**
     * List regional eSIM bundles filtered by region name
     * @param regionName The region name (e.g., "Europe", "Asia", "North America", etc.)
     */
    public ListBundlesResponse listRegionalBundlesByRegion(String regionName) {
        BundleResponse response = esimGoClient.listBundles();
        ListBundlesResponse result = new ListBundlesResponse();
        if (response != null && response.getBundles() != null) {
            result.setBundles(response.getBundles().stream()
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
                    .map(this::mapBundleToDto)
                    .collect(Collectors.toList()));
        }
        return result;
    }
    
    /**
     * Get list of available regions from regional bundles
     */
    public List<String> getAvailableRegions() {
        BundleResponse response = esimGoClient.listBundles();
        if (response != null && response.getBundles() != null) {
            return response.getBundles().stream()
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
        return new java.util.ArrayList<>();
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
     */
    public ListBundlesResponse listGlobalBundles() {
        BundleResponse response = esimGoClient.listBundles();
        ListBundlesResponse result = new ListBundlesResponse();
        if (response != null && response.getBundles() != null) {
            result.setBundles(response.getBundles().stream()
                    .filter(bundle -> {
                        if (bundle.getCountries() == null || bundle.getCountries().isEmpty()) {
                            return false;
                        }
                        
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
                    .map(this::mapBundleToDto)
                    .collect(Collectors.toList()));
        }
        return result;
    }
    
    private ListBundlesResponse mapToResponse(BundleResponse response) {
        ListBundlesResponse result = new ListBundlesResponse();
        if (response != null && response.getBundles() != null) {
            result.setBundles(response.getBundles().stream()
                    .map(this::mapBundleToDto)
                    .collect(Collectors.toList()));
        }
        return result;
    }
    
    private ListBundlesResponse.BundleDto mapBundleToDto(BundleResponse.Bundle bundle) {
        ListBundlesResponse.BundleDto dto = new ListBundlesResponse.BundleDto();
        dto.setName(bundle.getName());
        dto.setDescription(bundle.getDescription());
        dto.setDataAmount(bundle.getDataAmount());
        dto.setDuration(bundle.getDuration());
        dto.setAutostart(bundle.getAutostart());
        dto.setUnlimited(bundle.getUnlimited());
        dto.setRoamingEnabled(bundle.getRoamingEnabled());
        dto.setImageUrl(bundle.getImageUrl());
        dto.setPrice(bundle.getPrice());
        dto.setGroup(bundle.getGroup());
        dto.setBillingType(bundle.getBillingType());
        dto.setPotentialSpeeds(bundle.getPotentialSpeeds());
        
        if (bundle.getCountries() != null) {
            dto.setCountries(bundle.getCountries().stream()
                    .map(country -> {
                        ListBundlesResponse.CountryDto countryDto = new ListBundlesResponse.CountryDto();
                        countryDto.setName(country.getName());
                        countryDto.setRegion(country.getRegion());
                        countryDto.setIso(country.getIso());
                        return countryDto;
                    })
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }
}
