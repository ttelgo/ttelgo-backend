package com.tiktel.ttelgo.integration.esimgo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.util.List;

@Data
public class BundleResponse {
    private List<Bundle> bundles;
    
    @Data
    public static class Bundle {
        private String name;
        private String description;
        private List<Country> countries;
        
        @JsonProperty("dataAmount")
        private Integer dataAmount;
        
        private Integer duration;
        private Boolean autostart;
        private Boolean unlimited;
        
        @JsonProperty("roamingEnabled")
        @JsonDeserialize(using = RoamingEnabledDeserializer.class)
        private Boolean roamingEnabled;
        
        @JsonProperty("imageUrl")
        private String imageUrl;
        
        private Double price;
        private List<String> group;
        
        @JsonProperty("billingType")
        private String billingType;
        
        @JsonProperty("potentialSpeeds")
        private List<String> potentialSpeeds;
    }
    
    @Data
    public static class Country {
        private String name;
        private String region;
        private String iso;
    }
}

