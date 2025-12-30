package com.tiktel.ttelgo.plan.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class ListBundlesResponse {
    private List<BundleDto> bundles;
    
    @Data
    public static class BundleDto {
        private String name;
        private String description;
        private List<CountryDto> countries;
        private Integer dataAmount;
        private Integer duration;
        private Boolean autostart;
        private Boolean unlimited;
        private Boolean roamingEnabled;
        private String imageUrl;
        private Double price;
        private List<String> group;
        private String billingType;
        private List<String> potentialSpeeds;
    }
    
    @Data
    public static class CountryDto {
        private String name;
        private String region;
        private String iso;
    }
}

