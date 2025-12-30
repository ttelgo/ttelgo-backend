package com.tiktel.ttelgo.esim.infrastructure.adapter;

import com.tiktel.ttelgo.esim.application.port.EsimGoProvisioningPort;
import com.tiktel.ttelgo.integration.esimgo.EsimGoClient;
import com.tiktel.ttelgo.integration.esimgo.dto.CreateOrderRequest;
import com.tiktel.ttelgo.integration.esimgo.dto.CreateOrderResponse;
import com.tiktel.ttelgo.integration.esimgo.dto.OrderDetailResponse;
import com.tiktel.ttelgo.integration.esimgo.dto.QrCodeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EsimGoProvisioningAdapter implements EsimGoProvisioningPort {

    private final EsimGoClient esimGoClient;

    @Override
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        return esimGoClient.createOrder(request);
    }

    @Override
    public QrCodeResponse getQrCode(String matchingId) {
        return esimGoClient.getQrCode(matchingId);
    }

    @Override
    public OrderDetailResponse getOrderDetails(String orderId) {
        return esimGoClient.getOrderDetails(orderId);
    }
}

