package com.tiktel.ttelgo.integration.esimgo;

import com.tiktel.ttelgo.common.exception.BusinessException;
import com.tiktel.ttelgo.common.exception.ErrorCode;
import com.tiktel.ttelgo.integration.esimgo.domain.Bundle;
import com.tiktel.ttelgo.integration.esimgo.domain.OrderResult;
import com.tiktel.ttelgo.integration.esimgo.dto.BundleResponse;
import com.tiktel.ttelgo.integration.esimgo.dto.CreateOrderRequest;
import com.tiktel.ttelgo.integration.esimgo.dto.CreateOrderResponse;
import com.tiktel.ttelgo.integration.esimgo.mapper.EsimGoMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

/**
 * Service layer for eSIM Go integration with circuit breaker, retry, and caching
 * This is the anti-corruption layer that protects our domain from vendor API changes
 */
@Slf4j
@Service
public class EsimGoService {
    
    private final EsimGoClient esimGoClient;
    private final EsimGoMapper esimGoMapper;
    
    public EsimGoService(EsimGoClient esimGoClient, EsimGoMapper esimGoMapper) {
        this.esimGoClient = esimGoClient;
        this.esimGoMapper = esimGoMapper;
    }
    
    /**
     * Get all bundles with caching
     */
    @Cacheable(value = "bundles", key = "'all'", unless = "#result == null || #result.isEmpty()")
    @CircuitBreaker(name = "esimgo", fallbackMethod = "getBundlesFallback")
    @Retry(name = "esimgo")
    public List<Bundle> getBundles() {
        log.debug("Fetching all bundles from eSIM Go");
        
        try {
            BundleResponse response = esimGoClient.listBundles();
            if (response == null || response.getBundles() == null) {
                log.warn("eSIM Go returned null or empty bundle response");
                return List.of();
            }
            
            List<Bundle> bundles = esimGoMapper.toBundles(response.getBundles());
            log.info("Successfully fetched {} bundles from eSIM Go", bundles.size());
            return bundles;
            
        } catch (HttpClientErrorException e) {
            log.error("Client error from eSIM Go: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.ESIMGO_API_ERROR, 
                    "Failed to fetch bundles from eSIM Go", e);
        } catch (HttpServerErrorException e) {
            log.error("Server error from eSIM Go: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.ESIMGO_UNAVAILABLE, 
                    "eSIM Go service is temporarily unavailable", e);
        } catch (ResourceAccessException e) {
            log.error("Timeout or connection error to eSIM Go", e);
            throw new BusinessException(ErrorCode.ESIMGO_TIMEOUT, 
                    "Connection timeout to eSIM Go", e);
        }
    }
    
    /**
     * Get bundles with pagination
     */
    @CircuitBreaker(name = "esimgo", fallbackMethod = "getBundlesFallback")
    @Retry(name = "esimgo")
    public List<Bundle> getBundles(Integer page, Integer perPage, String direction, 
                                    String orderBy, String description) {
        log.debug("Fetching bundles from eSIM Go with filters: page={}, perPage={}, direction={}, orderBy={}, description={}", 
                page, perPage, direction, orderBy, description);
        
        try {
            BundleResponse response = esimGoClient.listBundles(page, perPage, direction, orderBy, description);
            if (response == null || response.getBundles() == null) {
                return List.of();
            }
            
            return esimGoMapper.toBundles(response.getBundles());
            
        } catch (Exception e) {
            handleEsimGoException(e, "fetch bundles");
            return List.of();
        }
    }
    
    /**
     * Get bundles by country
     */
    @Cacheable(value = "bundles", key = "'country_' + #countryIso", unless = "#result == null || #result.isEmpty()")
    @CircuitBreaker(name = "esimgo", fallbackMethod = "getBundlesByCountryFallback")
    @Retry(name = "esimgo")
    public List<Bundle> getBundlesByCountry(String countryIso) {
        log.debug("Fetching bundles for country: {}", countryIso);
        
        try {
            BundleResponse response = esimGoClient.listBundlesByCountry(countryIso);
            if (response == null || response.getBundles() == null) {
                return List.of();
            }
            
            return esimGoMapper.toBundles(response.getBundles());
            
        } catch (Exception e) {
            handleEsimGoException(e, "fetch bundles by country");
            return List.of();
        }
    }
    
    /**
     * Get bundle details by code
     */
    @Cacheable(value = "bundle-details", key = "#bundleCode", unless = "#result == null")
    @CircuitBreaker(name = "esimgo", fallbackMethod = "getBundleDetailsFallback")
    @Retry(name = "esimgo")
    public Bundle getBundleDetails(String bundleCode) {
        log.debug("Fetching bundle details for: {}", bundleCode);
        
        try {
            BundleResponse.Bundle response = esimGoClient.getBundleDetails(bundleCode);
            if (response == null) {
                throw new BusinessException(ErrorCode.BUNDLE_NOT_FOUND, 
                        "Bundle not found: " + bundleCode);
            }
            
            return esimGoMapper.toBundle(response);
            
        } catch (HttpClientErrorException.NotFound e) {
            throw new BusinessException(ErrorCode.BUNDLE_NOT_FOUND, 
                    "Bundle not found: " + bundleCode);
        } catch (Exception e) {
            handleEsimGoException(e, "fetch bundle details");
            return null;
        }
    }
    
    /**
     * Create order (purchase eSIM)
     * This is a critical operation - no caching, careful error handling
     */
    @CircuitBreaker(name = "esimgo", fallbackMethod = "createOrderFallback")
    @Retry(name = "esimgo")
    public OrderResult createOrder(String bundleCode, int quantity) {
        log.info("Creating order with eSIM Go: bundleCode={}, quantity={}", bundleCode, quantity);
        
        try {
            CreateOrderRequest.OrderItem item = new CreateOrderRequest.OrderItem();
            item.setType("bundle");
            item.setItem(bundleCode);
            item.setQuantity(quantity);
            item.setAllowReassign(false);
            
            CreateOrderRequest request = new CreateOrderRequest();
            request.setType("transaction");
            request.setAssign(true);
            request.setOrder(List.of(item));
            
            CreateOrderResponse response = esimGoClient.createOrder(request);
            if (response == null) {
                throw new BusinessException(ErrorCode.ESIMGO_INVALID_RESPONSE, 
                        "Invalid response from eSIM Go when creating order");
            }
            
            OrderResult result = esimGoMapper.toOrderResult(response);
            log.info("Successfully created order with eSIM Go: orderId={}, iccid={}", 
                    result.getOrderId(), result.getIccid());
            
            return result;
            
        } catch (HttpClientErrorException e) {
            log.error("Client error creating order with eSIM Go: {} - {}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            
            if (e.getStatusCode().value() == 400) {
                throw new BusinessException(ErrorCode.INVALID_BUNDLE_CODE, 
                        "Invalid bundle or insufficient stock", e);
            } else if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                throw new BusinessException(ErrorCode.ESIMGO_AUTHENTICATION_FAILED, 
                        "eSIM Go authentication failed", e);
            } else if (e.getStatusCode().value() == 429) {
                throw new BusinessException(ErrorCode.ESIMGO_RATE_LIMIT, 
                        "eSIM Go rate limit exceeded", e);
            }
            
            throw new BusinessException(ErrorCode.ESIMGO_API_ERROR, 
                    "Failed to create order with eSIM Go", e);
                    
        } catch (HttpServerErrorException e) {
            log.error("Server error creating order with eSIM Go: {} - {}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.ESIMGO_UNAVAILABLE, 
                    "eSIM Go service is temporarily unavailable", e);
                    
        } catch (ResourceAccessException e) {
            log.error("Timeout creating order with eSIM Go", e);
            throw new BusinessException(ErrorCode.ESIMGO_TIMEOUT, 
                    "Connection timeout to eSIM Go", e);
        }
    }
    
    /**
     * Get QR code for eSIM
     */
    @Cacheable(value = "qr-codes", key = "#matchingId", unless = "#result == null")
    @CircuitBreaker(name = "esimgo", fallbackMethod = "getQrCodeFallback")
    @Retry(name = "esimgo")
    public String getQrCode(String matchingId) {
        log.debug("Fetching QR code for matchingId: {}", matchingId);
        
        try {
            var response = esimGoClient.getQrCode(matchingId);
            if (response == null || response.getQrCode() == null) {
                throw new BusinessException(ErrorCode.QR_CODE_GENERATION_FAILED, 
                        "Failed to get QR code from eSIM Go");
            }
            
            return response.getQrCode();
            
        } catch (Exception e) {
            handleEsimGoException(e, "fetch QR code");
            return null;
        }
    }
    
    /**
     * Handle eSIM Go exceptions and convert to business exceptions
     */
    private void handleEsimGoException(Exception e, String operation) {
        if (e instanceof HttpClientErrorException) {
            HttpClientErrorException clientError = (HttpClientErrorException) e;
            log.error("Client error during {}: {} - {}", operation, 
                    clientError.getStatusCode(), clientError.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.ESIMGO_API_ERROR, 
                    "eSIM Go API error during " + operation, e);
                    
        } else if (e instanceof HttpServerErrorException) {
            log.error("Server error during {}", operation, e);
            throw new BusinessException(ErrorCode.ESIMGO_UNAVAILABLE, 
                    "eSIM Go service unavailable during " + operation, e);
                    
        } else if (e instanceof ResourceAccessException) {
            log.error("Timeout during {}", operation, e);
            throw new BusinessException(ErrorCode.ESIMGO_TIMEOUT, 
                    "Timeout during " + operation, e);
                    
        } else {
            log.error("Unexpected error during {}", operation, e);
            throw new BusinessException(ErrorCode.ESIMGO_API_ERROR, 
                    "Unexpected error during " + operation, e);
        }
    }
    
    // ==================== Fallback Methods ====================
    
    private List<Bundle> getBundlesFallback(Exception e) {
        log.error("Circuit breaker activated for getBundles, returning empty list", e);
        return List.of();
    }
    
    private List<Bundle> getBundlesByCountryFallback(String countryIso, Exception e) {
        log.error("Circuit breaker activated for getBundlesByCountry: {}, returning empty list", 
                countryIso, e);
        return List.of();
    }
    
    private Bundle getBundleDetailsFallback(String bundleCode, Exception e) {
        log.error("Circuit breaker activated for getBundleDetails: {}, returning null", 
                bundleCode, e);
        return null;
    }
    
    private OrderResult createOrderFallback(String bundleCode, int quantity, Exception e) {
        log.error("Circuit breaker activated for createOrder: bundleCode={}, quantity={}", 
                bundleCode, quantity, e);
        throw new BusinessException(ErrorCode.ESIMGO_UNAVAILABLE, 
                "eSIM Go service is currently unavailable. Please try again later.", e);
    }
    
    private String getQrCodeFallback(String matchingId, Exception e) {
        log.error("Circuit breaker activated for getQrCode: {}, returning null", matchingId, e);
        return null;
    }
}

