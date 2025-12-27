package com.tiktel.ttelgo.stripe.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class SessionDetailsResponse {
    private String sessionId;
    private String status;
    private String paymentStatus;
    private Long amountTotal;
    private String currency;
    private String paymentIntentId;
    private String customerEmail;
    private Map<String, String> metadata;
}

