package com.tiktel.ttelgo.esim.api;

import com.tiktel.ttelgo.esim.api.dto.ActivateBundleRequest;
import com.tiktel.ttelgo.esim.api.dto.ActivateBundleResponse;
import com.tiktel.ttelgo.esim.api.dto.EsimQrResponse;
import com.tiktel.ttelgo.esim.application.EsimService;
import com.tiktel.ttelgo.integration.esimgo.dto.QrCodeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/esims")
public class EsimController {
    
    private final EsimService esimService;
    
    @Autowired
    public EsimController(EsimService esimService) {
        this.esimService = esimService;
    }
    
    /**
     * Activate bundle (create order)
     */
    @PostMapping("/activate")
    public ResponseEntity<ActivateBundleResponse> activateBundle(
            @RequestBody ActivateBundleRequest request) {
        ActivateBundleResponse response = esimService.activateBundle(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get QR code by matching ID
     */
    @GetMapping("/qr/{matchingId}")
    public ResponseEntity<EsimQrResponse> getQrCode(
            @PathVariable String matchingId) {
        QrCodeResponse qrResponse = esimService.getQrCode(matchingId);
        
        EsimQrResponse response = new EsimQrResponse();
        response.setQrCode(qrResponse.getQrCode());
        response.setMatchingId(qrResponse.getMatchingId());
        response.setIccid(qrResponse.getIccid());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get order details by order ID
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ActivateBundleResponse> getOrderDetails(
            @PathVariable String orderId) {
        ActivateBundleResponse response = esimService.getOrderDetails(orderId);
        return ResponseEntity.ok(response);
    }
}
