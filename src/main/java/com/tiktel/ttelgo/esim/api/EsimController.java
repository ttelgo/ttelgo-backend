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

/**
 * eSIM API for customers
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/esims")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "eSIMs", description = "eSIM management")
public class EsimController {
    
    private final EsimService esimService;
    
    public EsimController(EsimService esimService) {
        this.esimService = esimService;
    }
    
    @Operation(summary = "Get my eSIMs", description = "Get all eSIMs for the current user")
    @GetMapping
    public ApiResponse<PageResponse<Esim>> getMyEsims(
            @ModelAttribute PageRequest pageRequest,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        log.info("Getting eSIMs for user: {}", userId);

        Page<Esim> esims = esimService.getEsimsForUser(userId, pageRequest.toPageable("createdAt"));
        PageResponse<Esim> response = PageResponse.of(esims.getContent(), esims);

        return ApiResponse.success(response);
    }

    @Operation(summary = "Get eSIM QR code", description = "Get QR code for eSIM activation")
    @GetMapping("/{iccid}/qr")
    public ApiResponse<QrCodeResponse> getQrCode(
            @PathVariable String iccid,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        log.info("Getting QR code: iccid={}, userId={}", iccid, userId);

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
