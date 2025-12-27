package com.tiktel.ttelgo.stripe.api.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class CreateCheckoutSessionRequest {
    /**
     * Existing order id if already created server-side.
     */
    private Long orderId;

    /**
     * Optional user id to attach to the order/payment (defaults to a fallback user).
     */
    private Long userId;

    /**
     * Optional currency override, defaults to USD.
     */
    private String currency;

    /**
     * Primary bundle/product identifier (used when no order exists yet).
     */
    private String bundleId;

    private String bundleName;

    @Positive
    private Integer quantity = 1;

    private String customerEmail;

    /**
     * Optional item list if the caller wants to send structured items.
     * Prices are always recalculated server-side.
     */
    private List<Item> items;

    @Data
    public static class Item {
        private String skuOrProductId;
        private String name;
        @Positive
        private Integer quantity;
    }
}

