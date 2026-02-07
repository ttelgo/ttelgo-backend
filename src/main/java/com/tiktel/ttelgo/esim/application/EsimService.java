package com.tiktel.ttelgo.esim.application;

<<<<<<< HEAD
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
=======
import com.tiktel.ttelgo.esim.api.dto.ActivateBundleRequest;
import com.tiktel.ttelgo.esim.api.dto.ActivateBundleResponse;
import com.tiktel.ttelgo.esim.application.port.EsimGoProvisioningPort;
import com.tiktel.ttelgo.esim.application.port.EsimRepositoryPort;
import com.tiktel.ttelgo.esim.domain.Esim;
import com.tiktel.ttelgo.esim.domain.EsimStatus;
import com.tiktel.ttelgo.integration.esimgo.dto.CreateOrderRequest;
import com.tiktel.ttelgo.integration.esimgo.dto.CreateOrderResponse;
import com.tiktel.ttelgo.integration.esimgo.dto.OrderDetailResponse;
import com.tiktel.ttelgo.integration.esimgo.dto.QrCodeResponse;
import com.tiktel.ttelgo.order.application.port.OrderRepositoryPort;
import com.tiktel.ttelgo.order.domain.Order;
import com.tiktel.ttelgo.order.domain.OrderStatus;
import com.tiktel.ttelgo.order.domain.PaymentStatus;
import org.springframework.beans.factory.annotation.Autowired;
>>>>>>> 517cfdbabcad5678433bdd3ff85dacd99c0cfaeb
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
    
<<<<<<< HEAD
    private final EsimRepository esimRepository;
    private final EsimMapper esimMapper;
    private final EsimGoService esimGoService;
    
    public EsimService(EsimRepository esimRepository,
                      EsimMapper esimMapper,
                      EsimGoService esimGoService) {
        this.esimRepository = esimRepository;
        this.esimMapper = esimMapper;
        this.esimGoService = esimGoService;
=======
    private final EsimGoProvisioningPort esimGoProvisioningPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final EsimRepositoryPort esimRepositoryPort;
    
    @Autowired
    public EsimService(EsimGoProvisioningPort esimGoProvisioningPort,
                      OrderRepositoryPort orderRepositoryPort,
                      EsimRepositoryPort esimRepositoryPort) {
        this.esimGoProvisioningPort = esimGoProvisioningPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.esimRepositoryPort = esimRepositoryPort;
>>>>>>> 517cfdbabcad5678433bdd3ff85dacd99c0cfaeb
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
        
<<<<<<< HEAD
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
=======
        // Step 3: Create ActivateBundleRequest from order
        ActivateBundleRequest request = new ActivateBundleRequest();
        request.setType("transaction");
        request.setAssign(true);
        request.setUserId(order.getUserId());
        
        ActivateBundleRequest.OrderItem orderItem = new ActivateBundleRequest.OrderItem();
        orderItem.setType("bundle");
        orderItem.setItem(order.getBundleId());
        orderItem.setQuantity(order.getQuantity());
        orderItem.setAllowReassign(false);
        request.setOrder(java.util.List.of(orderItem));
        
        // Step 4: Create order with eSIMGo API
        CreateOrderRequest createOrderRequest = mapToCreateOrderRequest(request);
        CreateOrderResponse esimGoResponse = esimGoProvisioningPort.createOrder(createOrderRequest);
        
        // Step 5: Update order and save eSIMs to database
        if (esimGoResponse != null && 
            (esimGoResponse.getStatus() != null && 
             (esimGoResponse.getStatus().equalsIgnoreCase("completed") || 
              esimGoResponse.getStatus().equalsIgnoreCase("success")))) {
            try {
                System.out.println("Saving order and eSIMs to database. Status: " + esimGoResponse.getStatus());
                updateOrderAndSaveEsimsToDatabase(order, esimGoResponse);
                System.out.println("Successfully saved order and eSIMs to database");
            } catch (Exception e) {
                System.err.println("Error saving order and eSIMs to database: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to save order and eSIMs to database", e);
            }
        } else {
            System.out.println("Skipping database save. Response status: " + 
                (esimGoResponse != null ? esimGoResponse.getStatus() : "null"));
        }
        
        // Step 6: Return response
        return mapToActivateBundleResponse(esimGoResponse);
    }
    
    /**
     * Legacy method - kept for backward compatibility
     * Now creates order but doesn't activate eSIM (payment required first)
     */
    @Transactional
    public ActivateBundleResponse activateBundle(ActivateBundleRequest request) {
        // This method is deprecated - payment should be processed first
        // For now, we'll still allow it but log a warning
        System.out.println("WARNING: activateBundle called without payment confirmation. " +
                "Consider using activateBundleAfterPayment instead.");
        
        // Step 1: Create order with eSIMGo API
        CreateOrderRequest createOrderRequest = mapToCreateOrderRequest(request);
        CreateOrderResponse esimGoResponse = esimGoProvisioningPort.createOrder(createOrderRequest);
        
        // Step 2: Save Order and Esim entities to database
        if (esimGoResponse != null && 
            (esimGoResponse.getStatus() != null && 
             (esimGoResponse.getStatus().equalsIgnoreCase("completed") || 
              esimGoResponse.getStatus().equalsIgnoreCase("success")))) {
            try {
                System.out.println("Saving order and eSIMs to database. Status: " + esimGoResponse.getStatus());
                saveOrderAndEsimsToDatabase(request, esimGoResponse);
                System.out.println("Successfully saved order and eSIMs to database");
            } catch (Exception e) {
                System.err.println("Error saving order and eSIMs to database: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to save order and eSIMs to database", e);
            }
        }
        
        // Step 3: Return response
        return mapToActivateBundleResponse(esimGoResponse);
    }
    
    /**
     * Save Order and Esim entities to database after successful eSIMGo activation
     */
    private void saveOrderAndEsimsToDatabase(ActivateBundleRequest request, CreateOrderResponse esimGoResponse) {
        System.out.println("=== Starting database save process ===");
        System.out.println("OrderReference: " + esimGoResponse.getOrderReference());
        System.out.println("Total: " + esimGoResponse.getTotal());
        System.out.println("Currency: " + esimGoResponse.getCurrency());
        System.out.println("UserId: " + request.getUserId());
        
        // Extract bundle information from request
        String bundleId = null;
        String bundleName = null;
        if (request.getOrder() != null && !request.getOrder().isEmpty()) {
            bundleId = request.getOrder().get(0).getItem();
            bundleName = request.getOrder().get(0).getItem(); // Bundle name is the same as bundle ID
            System.out.println("BundleId: " + bundleId);
            System.out.println("Quantity: " + request.getOrder().get(0).getQuantity());
        }
        
        // Validate orderReference is not null (required field)
        String orderReference = esimGoResponse.getOrderReference();
        if (orderReference == null || orderReference.trim().isEmpty()) {
            orderReference = UUID.randomUUID().toString();
            System.out.println("Warning: orderReference was null, generated new UUID: " + orderReference);
        }
        
        // Get price per unit
        BigDecimal unitPrice = BigDecimal.ZERO;
        if (esimGoResponse.getOrder() != null && !esimGoResponse.getOrder().isEmpty()) {
            Double pricePerUnit = esimGoResponse.getOrder().get(0).getPricePerUnit();
            if (pricePerUnit != null) {
                unitPrice = BigDecimal.valueOf(pricePerUnit);
            }
        }
        
        // Create and save Order entity
        Order order = Order.builder()
                .orderReference(orderReference)
                .userId(request.getUserId())
                .bundleId(bundleId)
                .bundleName(bundleName)
                .quantity(request.getOrder() != null && !request.getOrder().isEmpty() 
                    ? request.getOrder().get(0).getQuantity() 
                    : 1)
                .unitPrice(unitPrice)
                .totalAmount(esimGoResponse.getTotal() != null 
                    ? BigDecimal.valueOf(esimGoResponse.getTotal()) 
                    : BigDecimal.ZERO)
                .currency(esimGoResponse.getCurrency() != null ? esimGoResponse.getCurrency() : "USD")
                .status(OrderStatus.COMPLETED) // Order is completed after successful eSIMGo activation
                .paymentStatus(PaymentStatus.SUCCESS) // Payment is successful if eSIMGo accepted the order
                .esimgoOrderId(esimGoResponse.getOrderReference())
>>>>>>> 517cfdbabcad5678433bdd3ff85dacd99c0cfaeb
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
    
<<<<<<< HEAD
    /**
     * Activate eSIM
     */
    @Transactional
    public Esim activateEsim(String iccid) {
        log.info("Activating eSIM: iccid={}", iccid);
=======
    public QrCodeResponse getQrCode(String matchingId) {
        return esimGoProvisioningPort.getQrCode(matchingId);
    }
    
    public ActivateBundleResponse getOrderDetails(String orderId) {
        OrderDetailResponse response = esimGoProvisioningPort.getOrderDetails(orderId);
        return mapOrderDetailToResponse(response);
    }
    
    private CreateOrderRequest mapToCreateOrderRequest(ActivateBundleRequest request) {
        CreateOrderRequest createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setType(request.getType());
        createOrderRequest.setAssign(request.getAssign());
>>>>>>> 517cfdbabcad5678433bdd3ff85dacd99c0cfaeb
        
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
