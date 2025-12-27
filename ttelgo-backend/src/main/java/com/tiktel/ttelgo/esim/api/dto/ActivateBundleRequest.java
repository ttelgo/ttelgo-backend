package com.tiktel.ttelgo.esim.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class ActivateBundleRequest {
    private String type; // "transaction" or "validate"
    private Boolean assign;
    private Long userId; // User ID for database records
    private List<OrderItem> order;
    
    @Data
    public static class OrderItem {
        private String type; // "bundle"
        private String item; // bundle name
        private Integer quantity;
        private Boolean allowReassign;
    }
}

