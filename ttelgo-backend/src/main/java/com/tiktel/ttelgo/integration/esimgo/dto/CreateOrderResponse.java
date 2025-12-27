package com.tiktel.ttelgo.integration.esimgo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderResponse {
    private List<OrderDetail> order;
    private Double total;
    private String currency;
    private String status;
    
    @JsonProperty("statusMessage")
    private String statusMessage;
    
    @JsonProperty("orderReference")
    private String orderReference;
    
    @JsonProperty("createdDate")
    private String createdDate;
    
    private Boolean assigned;
    
    @Data
    public static class OrderDetail {
        private List<EsimInfo> esims;
        private String type;
        private String item;
        private List<String> iccids;
        private Integer quantity;
        
        @JsonProperty("subTotal")
        private Double subTotal;
        
        @JsonProperty("pricePerUnit")
        private Double pricePerUnit;
        
        @JsonProperty("AllowReassign")
        private Boolean allowReassign;
    }
    
    @Data
    public static class EsimInfo {
        private String iccid;
        
        @JsonProperty("matchingId")
        private String matchingId;
        
        @JsonProperty("smdpAddress")
        private String smdpAddress;
    }
}

