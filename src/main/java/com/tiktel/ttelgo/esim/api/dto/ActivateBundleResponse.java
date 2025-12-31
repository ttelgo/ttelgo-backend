package com.tiktel.ttelgo.esim.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class ActivateBundleResponse {
    private List<OrderDetail> order;
    private Double total;
    private String currency;
    private String status;
    private String statusMessage;
    private String orderReference;
    private String createdDate;
    private Boolean assigned;
    
    @Data
    public static class OrderDetail {
        private List<EsimInfo> esims;
        private String type;
        private String item;
        private List<String> iccids;
        private Integer quantity;
        private Double subTotal;
        private Double pricePerUnit;
        private Boolean allowReassign;
    }
    
    @Data
    public static class EsimInfo {
        private String iccid;
        private String matchingId;
        private String smdpAddress;
    }
}

