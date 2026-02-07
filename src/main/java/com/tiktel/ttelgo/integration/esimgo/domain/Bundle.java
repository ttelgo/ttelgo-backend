package com.tiktel.ttelgo.integration.esimgo.domain;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Internal domain model for Bundle (anti-corruption layer)
 * This is our internal representation, isolated from eSIM Go's API structure
 */
@Data
@Builder
public class Bundle {
    private String code;
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private String dataAmount;
    private Integer validityDays;
    private List<String> countries;
    private String countryCode;
    private String regionName;
    private String groupName;
    private Boolean roamingEnabled;
    private String networkType; // e.g., "4G", "5G"
    private Boolean available;
    private String imageUrl;
}

