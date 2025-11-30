package com.tiktel.ttelgo.esim.application;

import com.tiktel.ttelgo.esim.api.dto.ActivateBundleRequest;
import com.tiktel.ttelgo.esim.api.dto.ActivateBundleResponse;
import com.tiktel.ttelgo.integration.esimgo.EsimGoClient;
import com.tiktel.ttelgo.integration.esimgo.dto.CreateOrderRequest;
import com.tiktel.ttelgo.integration.esimgo.dto.CreateOrderResponse;
import com.tiktel.ttelgo.integration.esimgo.dto.OrderDetailResponse;
import com.tiktel.ttelgo.integration.esimgo.dto.QrCodeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class EsimService {
    
    private final EsimGoClient esimGoClient;
    
    @Autowired
    public EsimService(EsimGoClient esimGoClient) {
        this.esimGoClient = esimGoClient;
    }
    
    public ActivateBundleResponse activateBundle(ActivateBundleRequest request) {
        CreateOrderRequest createOrderRequest = mapToCreateOrderRequest(request);
        CreateOrderResponse response = esimGoClient.createOrder(createOrderRequest);
        return mapToActivateBundleResponse(response);
    }
    
    public QrCodeResponse getQrCode(String matchingId) {
        return esimGoClient.getQrCode(matchingId);
    }
    
    public ActivateBundleResponse getOrderDetails(String orderId) {
        OrderDetailResponse response = esimGoClient.getOrderDetails(orderId);
        return mapOrderDetailToResponse(response);
    }
    
    private CreateOrderRequest mapToCreateOrderRequest(ActivateBundleRequest request) {
        CreateOrderRequest createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setType(request.getType());
        createOrderRequest.setAssign(request.getAssign());
        
        if (request.getOrder() != null) {
            createOrderRequest.setOrder(request.getOrder().stream()
                    .map(item -> {
                        CreateOrderRequest.OrderItem orderItem = new CreateOrderRequest.OrderItem();
                        orderItem.setType(item.getType());
                        orderItem.setItem(item.getItem());
                        orderItem.setQuantity(item.getQuantity());
                        orderItem.setAllowReassign(item.getAllowReassign());
                        return orderItem;
                    })
                    .collect(Collectors.toList()));
        }
        
        return createOrderRequest;
    }
    
    private ActivateBundleResponse mapToActivateBundleResponse(CreateOrderResponse response) {
        ActivateBundleResponse result = new ActivateBundleResponse();
        result.setTotal(response.getTotal());
        result.setCurrency(response.getCurrency());
        result.setStatus(response.getStatus());
        result.setStatusMessage(response.getStatusMessage());
        result.setOrderReference(response.getOrderReference());
        result.setCreatedDate(response.getCreatedDate());
        result.setAssigned(response.getAssigned());
        
        if (response.getOrder() != null) {
            result.setOrder(response.getOrder().stream()
                    .map(orderDetail -> {
                        ActivateBundleResponse.OrderDetail detail = new ActivateBundleResponse.OrderDetail();
                        detail.setType(orderDetail.getType());
                        detail.setItem(orderDetail.getItem());
                        detail.setIccids(orderDetail.getIccids());
                        detail.setQuantity(orderDetail.getQuantity());
                        detail.setSubTotal(orderDetail.getSubTotal());
                        detail.setPricePerUnit(orderDetail.getPricePerUnit());
                        detail.setAllowReassign(orderDetail.getAllowReassign());
                        
                        if (orderDetail.getEsims() != null) {
                            detail.setEsims(orderDetail.getEsims().stream()
                                    .map(esim -> {
                                        ActivateBundleResponse.EsimInfo esimInfo = new ActivateBundleResponse.EsimInfo();
                                        esimInfo.setIccid(esim.getIccid());
                                        esimInfo.setMatchingId(esim.getMatchingId());
                                        esimInfo.setSmdpAddress(esim.getSmdpAddress());
                                        return esimInfo;
                                    })
                                    .collect(Collectors.toList()));
                        }
                        
                        return detail;
                    })
                    .collect(Collectors.toList()));
        }
        
        return result;
    }
    
    private ActivateBundleResponse mapOrderDetailToResponse(OrderDetailResponse response) {
        ActivateBundleResponse result = new ActivateBundleResponse();
        result.setTotal(response.getTotal());
        result.setCurrency(response.getCurrency());
        result.setStatus(response.getStatus());
        result.setStatusMessage(response.getStatusMessage());
        result.setOrderReference(response.getOrderReference());
        result.setCreatedDate(response.getCreatedDate());
        result.setAssigned(response.getAssigned());
        
        if (response.getOrder() != null) {
            result.setOrder(response.getOrder().stream()
                    .map(orderDetail -> {
                        ActivateBundleResponse.OrderDetail detail = new ActivateBundleResponse.OrderDetail();
                        detail.setType(orderDetail.getType());
                        detail.setItem(orderDetail.getItem());
                        detail.setIccids(orderDetail.getIccids());
                        detail.setQuantity(orderDetail.getQuantity());
                        detail.setSubTotal(orderDetail.getSubTotal());
                        detail.setPricePerUnit(orderDetail.getPricePerUnit());
                        detail.setAllowReassign(orderDetail.getAllowReassign());
                        
                        if (orderDetail.getEsims() != null) {
                            detail.setEsims(orderDetail.getEsims().stream()
                                    .map(esim -> {
                                        ActivateBundleResponse.EsimInfo esimInfo = new ActivateBundleResponse.EsimInfo();
                                        esimInfo.setIccid(esim.getIccid());
                                        esimInfo.setMatchingId(esim.getMatchingId());
                                        esimInfo.setSmdpAddress(esim.getSmdpAddress());
                                        return esimInfo;
                                    })
                                    .collect(Collectors.toList()));
                        }
                        
                        return detail;
                    })
                    .collect(Collectors.toList()));
        }
        
        return result;
    }
}
