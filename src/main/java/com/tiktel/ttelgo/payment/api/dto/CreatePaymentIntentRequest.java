package com.tiktel.ttelgo.payment.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePaymentIntentRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    private String currency; // e.g., "usd"
    
    private Long orderId; // Optional: for existing order
    
    private String customerEmail; // Optional: customer email
    
    private String bundleId; // Bundle ID for the eSIM
    
    private String bundleName; // Bundle name
    
    private Integer quantity; // Quantity of bundles
}

