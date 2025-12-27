package com.tiktel.ttelgo.stripe.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCheckoutSessionResponse {
    private String sessionId;
    private String url;
    private Long orderId;
    private String paymentStatus;
}

