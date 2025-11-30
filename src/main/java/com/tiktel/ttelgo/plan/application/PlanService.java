package com.tiktel.ttelgo.plan.application;

import com.tiktel.ttelgo.integration.esimgo.EsimGoClient;
import com.tiktel.ttelgo.integration.esimgo.dto.BundleResponse;
import com.tiktel.ttelgo.plan.api.dto.ListBundlesResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    
    public ListBundlesResponse listBundlesByCountry(String countryIso) {
        BundleResponse response = esimGoClient.listBundlesByCountry(countryIso);
        return mapToResponse(response);
    }
    
    public ListBundlesResponse.BundleDto getBundleDetails(String bundleName) {
        BundleResponse.Bundle bundle = esimGoClient.getBundleDetails(bundleName);
        return mapBundleToDto(bundle);
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
