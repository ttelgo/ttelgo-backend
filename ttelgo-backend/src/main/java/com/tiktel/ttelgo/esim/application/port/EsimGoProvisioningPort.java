package com.tiktel.ttelgo.esim.application.port;

import com.tiktel.ttelgo.integration.esimgo.dto.CreateOrderRequest;
import com.tiktel.ttelgo.integration.esimgo.dto.CreateOrderResponse;
import com.tiktel.ttelgo.integration.esimgo.dto.OrderDetailResponse;
import com.tiktel.ttelgo.integration.esimgo.dto.QrCodeResponse;

public interface EsimGoProvisioningPort {

    CreateOrderResponse createOrder(CreateOrderRequest request);

    QrCodeResponse getQrCode(String matchingId);

    OrderDetailResponse getOrderDetails(String orderId);
}

