package com.tiktel.ttelgo.esim.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.dto.PageRequest;
import com.tiktel.ttelgo.common.dto.PageResponse;
import com.tiktel.ttelgo.esim.application.EsimService;
import com.tiktel.ttelgo.esim.domain.Esim;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * eSIM API for customers
 */
@Slf4j
@RestController
<<<<<<< HEAD
@RequestMapping("/api/v1/esims")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "eSIMs", description = "eSIM management")
=======
@RequestMapping("/api/v1")
>>>>>>> 517cfdbabcad5678433bdd3ff85dacd99c0cfaeb
public class EsimController {
    
    private final EsimService esimService;
    
    public EsimController(EsimService esimService) {
        this.esimService = esimService;
    }
    
<<<<<<< HEAD
    @Operation(summary = "Get my eSIMs", description = "Get all eSIMs for the current user")
    @GetMapping
    public ApiResponse<PageResponse<Esim>> getMyEsims(
            @ModelAttribute PageRequest pageRequest,
            Authentication authentication) {
=======
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
>>>>>>> 517cfdbabcad5678433bdd3ff85dacd99c0cfaeb
        
        Long userId = extractUserId(authentication);
        log.info("Getting eSIMs for user: {}", userId);
        
        Page<Esim> esims = esimService.getEsimsForUser(userId, pageRequest.toPageable("createdAt"));
        PageResponse<Esim> response = PageResponse.of(esims.getContent(), esims);
        
        return ApiResponse.success(response);
    }
    
<<<<<<< HEAD
    @Operation(summary = "Get eSIM QR code", description = "Get QR code for eSIM activation")
    @GetMapping("/{iccid}/qr")
    public ApiResponse<QrCodeResponse> getQrCode(
            @PathVariable String iccid,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        log.info("Getting QR code: iccid={}, userId={}", iccid, userId);
        
        // Verify eSIM belongs to user
        Esim esim = esimService.getEsimByIccid(iccid);
        if (!esim.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        String qrCode = esimService.getQrCode(iccid);
        
        return ApiResponse.success(new QrCodeResponse(
                iccid,
                esim.getMatchingId(),
                esim.getSmdpAddress(),
                esim.getActivationCode(),
                qrCode
        ));
=======
    /**
     * Get eSIM provider order details by provider order ID.
     * GET /api/v1/esim-orders/{orderId}
     */
    @GetMapping("/esim-orders/{orderId}")
    public ResponseEntity<ActivateBundleResponse> getEsimOrderDetails(@PathVariable String orderId) {
        ActivateBundleResponse response = esimService.getOrderDetails(orderId);
        return ResponseEntity.ok(response);
>>>>>>> 517cfdbabcad5678433bdd3ff85dacd99c0cfaeb
    }
    
    @Operation(summary = "Get eSIM status", description = "Get eSIM details and status")
    @GetMapping("/{iccid}/status")
    public ApiResponse<Esim> getEsimStatus(
            @PathVariable String iccid,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        log.info("Getting eSIM status: iccid={}", iccid);
        
        Esim esim = esimService.getEsimByIccid(iccid);
        
        // Verify eSIM belongs to user
        if (!esim.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        return ApiResponse.success(esim);
    }
    
    private Long extractUserId(Authentication authentication) {
        // TODO: Extract user ID from JWT token
        return 1L; // Placeholder
    }
    
    public record QrCodeResponse(
            String iccid,
            String matchingId,
            String smdpAddress,
            String activationCode,
            String qrCode
    ) {}
}
