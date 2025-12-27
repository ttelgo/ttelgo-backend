package com.tiktel.ttelgo.integration.esimgo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class OrderDetailResponse {
    private List<CreateOrderResponse.OrderDetail> order;
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
}

