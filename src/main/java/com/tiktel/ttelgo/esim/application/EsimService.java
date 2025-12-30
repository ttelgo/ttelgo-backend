package com.tiktel.ttelgo.esim.application;

import com.tiktel.ttelgo.common.domain.enums.EsimStatus;
import com.tiktel.ttelgo.common.exception.BusinessException;
import com.tiktel.ttelgo.common.exception.ErrorCode;
import com.tiktel.ttelgo.common.exception.ResourceNotFoundException;
import com.tiktel.ttelgo.esim.domain.Esim;
import com.tiktel.ttelgo.esim.infrastructure.mapper.EsimMapper;
import com.tiktel.ttelgo.esim.infrastructure.repository.EsimJpaEntity;
import com.tiktel.ttelgo.esim.infrastructure.repository.EsimRepository;
import com.tiktel.ttelgo.integration.esimgo.EsimGoService;
import com.tiktel.ttelgo.integration.esimgo.domain.OrderResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * eSIM service for managing eSIM inventory
 */
@Slf4j
@Service
public class EsimService {
    
    private final EsimRepository esimRepository;
    private final EsimMapper esimMapper;
    private final EsimGoService esimGoService;
    
    public EsimService(EsimRepository esimRepository,
                      EsimMapper esimMapper,
                      EsimGoService esimGoService) {
        this.esimRepository = esimRepository;
        this.esimMapper = esimMapper;
        this.esimGoService = esimGoService;
    }
    
    /**
     * Create eSIM records from eSIM Go order result
     */
    @Transactional
    public Esim createEsimFromOrderResult(Long orderId, Long userId, Long vendorId,
                                          OrderResult orderResult) {
        log.info("Creating eSIM record: orderId={}, iccid={}", orderId, orderResult.getIccid());
        
        // Check if eSIM already exists
        if (esimRepository.existsByIccid(orderResult.getIccid())) {
            log.warn("eSIM already exists: iccid={}", orderResult.getIccid());
            return esimRepository.findByIccid(orderResult.getIccid())
                    .map(esimMapper::toDomain)
                    .orElseThrow();
        }
        
        // Create eSIM entity
        EsimJpaEntity esim = EsimJpaEntity.builder()
                .orderId(orderId)
                .userId(userId)
                .vendorId(vendorId)
                .iccid(orderResult.getIccid())
                .matchingId(orderResult.getMatchingId())
                .smdpAddress(orderResult.getSmdpAddress())
                .activationCode(orderResult.getActivationCode())
                .qrCodeUrl(orderResult.getQrCodeUrl())
                .bundleCode(orderResult.getBundleCode())
                .bundleName(orderResult.getBundleName())
                .status(EsimStatus.CREATED)
                .dataUsedBytes(0L)
                .countryIso(orderResult.getOrderId() != null ? extractCountryFromBundle(orderResult.getBundleCode()) : null)
                .syncStatus("SYNCED")
                .lastSyncedAt(LocalDateTime.now())
                .build();
        
        EsimJpaEntity saved = esimRepository.save(esim);
        log.info("eSIM created: id={}, iccid={}", saved.getId(), saved.getIccid());
        
        return esimMapper.toDomain(saved);
    }
    
    /**
     * Get eSIM by ICCID
     */
    public Esim getEsimByIccid(String iccid) {
        EsimJpaEntity esim = esimRepository.findByIccid(iccid)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ESIM_NOT_FOUND,
                        "eSIM not found with ICCID: " + iccid));
        return esimMapper.toDomain(esim);
    }
    
    /**
     * Get eSIMs for order
     */
    public List<Esim> getEsimsByOrderId(Long orderId) {
        return esimRepository.findByOrderId(orderId).stream()
                .map(esimMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    /**
     * Get eSIMs for user (B2C)
     */
    public Page<Esim> getEsimsForUser(Long userId, Pageable pageable) {
        return esimRepository.findByUserId(userId, pageable)
                .map(esimMapper::toDomain);
    }
    
    /**
     * Get eSIMs for vendor (B2B)
     */
    public Page<Esim> getEsimsForVendor(Long vendorId, Pageable pageable) {
        return esimRepository.findByVendorId(vendorId, pageable)
                .map(esimMapper::toDomain);
    }
    
    /**
     * Get QR code for eSIM (with caching)
     */
    @Cacheable(value = "esim-qr-codes", key = "#iccid")
    public String getQrCode(String iccid) {
        log.info("Getting QR code for eSIM: iccid={}", iccid);
        
        EsimJpaEntity esim = esimRepository.findByIccid(iccid)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ESIM_NOT_FOUND,
                        "eSIM not found with ICCID: " + iccid));
        
        // Return cached QR code if available
        if (esim.getQrCode() != null && !esim.getQrCode().isEmpty()) {
            log.info("Returning cached QR code for iccid: {}", iccid);
            return esim.getQrCode();
        }
        
        // Fetch from eSIM Go
        if (esim.getMatchingId() != null) {
            try {
                String qrCode = esimGoService.getQrCode(esim.getMatchingId());
                
                // Cache the QR code
                esim.setQrCode(qrCode);
                esimRepository.save(esim);
                
                return qrCode;
            } catch (Exception e) {
                log.error("Failed to fetch QR code from eSIM Go: iccid={}", iccid, e);
                throw new BusinessException(ErrorCode.QR_CODE_GENERATION_FAILED,
                        "Failed to get QR code", e);
            }
        }
        
        throw new BusinessException(ErrorCode.QR_CODE_GENERATION_FAILED,
                "QR code not available for this eSIM");
    }
    
    /**
     * Activate eSIM
     */
    @Transactional
    public Esim activateEsim(String iccid) {
        log.info("Activating eSIM: iccid={}", iccid);
        
        EsimJpaEntity esim = esimRepository.findByIccid(iccid)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ESIM_NOT_FOUND,
                        "eSIM not found with ICCID: " + iccid));
        
        if (esim.getStatus() == EsimStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ESIM_ALREADY_ACTIVATED,
                    "eSIM is already activated");
        }
        
        esim.setStatus(EsimStatus.ACTIVE);
        esim.setActivatedAt(LocalDateTime.now());
        
        EsimJpaEntity saved = esimRepository.save(esim);
        log.info("eSIM activated: iccid={}", iccid);
        
        return esimMapper.toDomain(saved);
    }
    
    /**
     * Update eSIM usage
     */
    @Transactional
    public Esim updateUsage(String iccid, Long dataUsedBytes) {
        log.info("Updating eSIM usage: iccid={}, dataUsedBytes={}", iccid, dataUsedBytes);
        
        EsimJpaEntity esim = esimRepository.findByIccid(iccid)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ESIM_NOT_FOUND,
                        "eSIM not found with ICCID: " + iccid));
        
        esim.setDataUsedBytes(dataUsedBytes);
        
        if (esim.getDataLimitBytes() != null) {
            esim.setDataRemainingBytes(esim.getDataLimitBytes() - dataUsedBytes);
        }
        
        esim.setLastSyncedAt(LocalDateTime.now());
        
        EsimJpaEntity saved = esimRepository.save(esim);
        return esimMapper.toDomain(saved);
    }
    
    /**
     * Mark expired eSIMs
     */
    @Transactional
    public int markExpiredEsims() {
        List<EsimJpaEntity> expiredEsims = esimRepository.findExpiredEsims(LocalDateTime.now());
        
        for (EsimJpaEntity esim : expiredEsims) {
            esim.setStatus(EsimStatus.EXPIRED);
            esim.setExpiredAt(LocalDateTime.now());
            esimRepository.save(esim);
        }
        
        int count = expiredEsims.size();
        if (count > 0) {
            log.info("Marked {} eSIMs as expired", count);
        }
        
        return count;
    }
    
    /**
     * Extract country code from bundle code (simple heuristic)
     */
    private String extractCountryFromBundle(String bundleCode) {
        if (bundleCode == null || bundleCode.length() < 2) {
            return null;
        }
        // Simple pattern: BUNDLE_US_5GB_30D -> US
        String[] parts = bundleCode.split("_");
        if (parts.length >= 2 && parts[1].length() == 2) {
            return parts[1].toUpperCase();
        }
        return null;
    }
}
