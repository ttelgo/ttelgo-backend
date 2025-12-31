package com.tiktel.ttelgo.esim.api;

import com.tiktel.ttelgo.esim.api.dto.ActivateBundleRequest;
import com.tiktel.ttelgo.esim.api.dto.ActivateBundleResponse;
import com.tiktel.ttelgo.esim.api.dto.EsimQrResponse;
import com.tiktel.ttelgo.esim.application.EsimService;
import com.tiktel.ttelgo.integration.esimgo.dto.QrCodeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EsimController {
    
    private final EsimService esimService;
    
    /**
     * Create eSIMs for an existing order after payment confirmation.
     * POST /api/v1/orders/{orderId}/esims
     */
    @PostMapping("/orders/{orderId}/esims")
    public ResponseEntity<ActivateBundleResponse> provisionEsimsForOrder(@PathVariable Long orderId) {
        ActivateBundleResponse response = esimService.activateBundleAfterPayment(orderId);
        return ResponseEntity.status(201).body(response);
    }
    
    /**
     * Create an eSIM order directly with the provider (legacy flow).
     * POST /api/v1/esim-orders
     */
    @PostMapping("/esim-orders")
    public ResponseEntity<ActivateBundleResponse> createEsimOrder(@RequestBody ActivateBundleRequest request) {
        ActivateBundleResponse response = esimService.activateBundle(request);
        return ResponseEntity.status(201).body(response);
    }
    
    /**
     * Get QR code by matching ID.
     * GET /api/v1/esims/{matchingId}/qr
     */
    @GetMapping("/esims/{matchingId}/qr")
    public ResponseEntity<EsimQrResponse> getQrCode(@PathVariable String matchingId) {
        QrCodeResponse qrResponse = esimService.getQrCode(matchingId);
        EsimQrResponse response = new EsimQrResponse();
        response.setQrCode(qrResponse.getQrCode());
        response.setMatchingId(qrResponse.getMatchingId());
        response.setIccid(qrResponse.getIccid());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get eSIM provider order details by provider order ID.
     * GET /api/v1/esim-orders/{orderId}
     */
    @GetMapping("/esim-orders/{orderId}")
    public ResponseEntity<ActivateBundleResponse> getEsimOrderDetails(@PathVariable String orderId) {
        ActivateBundleResponse response = esimService.getOrderDetails(orderId);
        return ResponseEntity.ok(response);
    }
}
