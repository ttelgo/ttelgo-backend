package com.tiktel.ttelgo.integration.esimgo.mapper;

import com.tiktel.ttelgo.integration.esimgo.domain.Bundle;
import com.tiktel.ttelgo.integration.esimgo.domain.OrderResult;
import com.tiktel.ttelgo.integration.esimgo.dto.BundleResponse;
import com.tiktel.ttelgo.integration.esimgo.dto.CreateOrderResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for eSIM Go DTOs - Anti-Corruption Layer
 */
@Component
public class EsimGoMapper {
    
    /**
     * Convert eSIM Go bundle DTO to domain model
     */
    public Bundle toBundle(BundleResponse.Bundle esimGoBundle) {
        if (esimGoBundle == null) {
            return null;
        }
        
        return Bundle.builder()
                .code(esimGoBundle.getName()) // Use name as code
                .name(esimGoBundle.getName())
                .description(esimGoBundle.getDescription())
                .price(BigDecimal.valueOf(esimGoBundle.getPrice() != null ? esimGoBundle.getPrice() : 0.0))
                .currency("USD") // Default currency
                .dataAmount(esimGoBundle.getDataAmount() != null ? esimGoBundle.getDataAmount().toString() + "GB" : "Unknown")
                .validityDays(esimGoBundle.getDuration())
                .countries(esimGoBundle.getCountries() != null && !esimGoBundle.getCountries().isEmpty() ? 
                           esimGoBundle.getCountries().stream().map(c -> c.getName()).collect(Collectors.toList()) : List.of("Unknown"))
                .countryCode(esimGoBundle.getCountries() != null && !esimGoBundle.getCountries().isEmpty() ? 
                            esimGoBundle.getCountries().get(0).getIso() : null)
                .networkType("LTE") // Default
                .roamingEnabled(esimGoBundle.getRoamingEnabled())
                .available(true) // Assume available if returned
                .build();
    }
    
    /**
     * Convert list of eSIM Go bundles to domain models
     */
    public List<Bundle> toBundles(List<BundleResponse.Bundle> esimGoBundles) {
        if (esimGoBundles == null) {
            return List.of();
        }
        
        return esimGoBundles.stream()
                .map(this::toBundle)
                .collect(Collectors.toList());
    }
    
    /**
     * Convert eSIM Go order response to domain model
     */
    public OrderResult toOrderResult(CreateOrderResponse esimGoResponse) {
        if (esimGoResponse == null) {
            return null;
        }
        
        // Extract first eSIM from response
        String iccid = extractIccid(esimGoResponse);
        String matchingId = extractMatchingId(esimGoResponse);
        String smdpAddress = extractSmdpAddress(esimGoResponse);
        
        return OrderResult.builder()
                .orderId(esimGoResponse.getOrderReference())
                .iccid(iccid)
                .matchingId(matchingId)
                .smdpAddress(smdpAddress)
                .activationCode(buildActivationCode(smdpAddress, matchingId))
                .bundleCode(extractBundleCode(esimGoResponse))
                .bundleName(extractBundleCode(esimGoResponse))
                .price(BigDecimal.valueOf(esimGoResponse.getTotal() != null ? esimGoResponse.getTotal() : 0.0))
                .currency(esimGoResponse.getCurrency())
                .status(esimGoResponse.getStatus())
                .createdAt(null) // Parse date if needed
                .qrCodeUrl(buildQrCodeUrl(smdpAddress, matchingId))
                .build();
    }
    
    // Helper methods
    
    private String extractBundleCode(CreateOrderResponse response) {
        if (response.getOrder() != null && !response.getOrder().isEmpty()) {
            return response.getOrder().get(0).getItem();
        }
        return null;
    }
    
    private String extractIccid(CreateOrderResponse response) {
        if (response.getOrder() != null && !response.getOrder().isEmpty()) {
            CreateOrderResponse.OrderDetail detail = response.getOrder().get(0);
            if (detail.getEsims() != null && !detail.getEsims().isEmpty()) {
                return detail.getEsims().get(0).getIccid();
            }
        }
        return null;
    }
    
    private String extractMatchingId(CreateOrderResponse response) {
        if (response.getOrder() != null && !response.getOrder().isEmpty()) {
            CreateOrderResponse.OrderDetail detail = response.getOrder().get(0);
            if (detail.getEsims() != null && !detail.getEsims().isEmpty()) {
                return detail.getEsims().get(0).getMatchingId();
            }
        }
        return null;
    }
    
    private String extractSmdpAddress(CreateOrderResponse response) {
        if (response.getOrder() != null && !response.getOrder().isEmpty()) {
            CreateOrderResponse.OrderDetail detail = response.getOrder().get(0);
            if (detail.getEsims() != null && !detail.getEsims().isEmpty()) {
                return detail.getEsims().get(0).getSmdpAddress();
            }
        }
        return null;
    }
    
    private String buildActivationCode(String smdpAddress, String matchingId) {
        if (smdpAddress != null && matchingId != null) {
            return String.format("LPA:%s$%s", smdpAddress, matchingId);
        }
        return null;
    }
    
    private String buildQrCodeUrl(String smdpAddress, String matchingId) {
        if (smdpAddress != null && matchingId != null) {
            // Construct QR code URL (this is a placeholder - actual URL depends on eSIM Go API)
            return String.format("https://api.esim-go.com/v2/qrcode?smdp=%s&matching=%s", smdpAddress, matchingId);
        }
        return null;
    }
}

