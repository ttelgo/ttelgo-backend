package com.tiktel.ttelgo.integration.esimgo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    private String type; // "transaction" or "validate"
    
    @JsonProperty("assign")
    private Boolean assign;
    
    private List<OrderItem> order;
    
    @Data
    public static class OrderItem {
        private String type; // "bundle"
        private String item; // bundle name
        private Integer quantity;
        
        @JsonProperty("allowReassign")
        private Boolean allowReassign;
    }
}

