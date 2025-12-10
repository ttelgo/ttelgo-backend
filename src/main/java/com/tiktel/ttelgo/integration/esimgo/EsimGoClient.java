package com.tiktel.ttelgo.integration.esimgo;

import com.tiktel.ttelgo.integration.esimgo.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class EsimGoClient {
    
    private final EsimGoConfig config;
    private final RestTemplate restTemplate;
    
    @Autowired
    public EsimGoClient(EsimGoConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }
    
    /**
     * List all available bundles
     */
    public BundleResponse listBundles() {
        String url = config.getApiEndpoint() + "/catalogue";
        return executeGet(url, BundleResponse.class);
    }
    
    /**
     * List all available bundles with pagination
     * @param page Page number (default: 1)
     * @param perPage Number of bundles per page (default: 50)
     * @param direction Sort direction (asc/desc, default: asc)
     * @param orderBy Column to order by
     * @param description Search term for description (wildcard search)
     */
    public BundleResponse listBundles(Integer page, Integer perPage, String direction, String orderBy, String description) {
        String url = config.getApiEndpoint() + "/catalogue";
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        
        if (page != null) {
            builder.queryParam("page", page);
        }
        if (perPage != null) {
            builder.queryParam("perPage", perPage);
        }
        if (direction != null && !direction.isEmpty()) {
            builder.queryParam("direction", direction);
        }
        if (orderBy != null && !orderBy.isEmpty()) {
            builder.queryParam("orderBy", orderBy);
        }
        if (description != null && !description.trim().isEmpty()) {
            builder.queryParam("description", description.trim());
        }
        
        return executeGet(builder.toUriString(), BundleResponse.class);
    }
    
    /**
     * List bundles by country ISO code (e.g., "GB", "US")
     */
    public BundleResponse listBundlesByCountry(String countryIso) {
        String url = config.getApiEndpoint() + "/catalogue";
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url)
                .queryParam("countries", countryIso);
        
        return executeGet(builder.toUriString(), BundleResponse.class);
    }
    
    /**
     * List bundles by group name (e.g., "Standard eSIM Bundles", "Regional Bundles", "Global Bundles")
     * According to eSIMGo API docs: https://docs.esim-go.com/api/v2_4/operations/catalogue/get/
     */
    public BundleResponse listBundlesByGroup(String groupName) {
        String url = config.getApiEndpoint() + "/catalogue";
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url)
                .queryParam("group", groupName);
        
        return executeGet(builder.toUriString(), BundleResponse.class);
    }
    
    /**
     * Get bundle details by bundle name
     */
    public BundleResponse.Bundle getBundleDetails(String bundleName) {
        String url = config.getApiEndpoint() + "/catalogue/bundle/" + bundleName;
        return executeGet(url, BundleResponse.Bundle.class);
    }
    
    /**
     * Create order (activate bundle)
     */
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        String url = config.getApiEndpoint() + "/orders";
        return executePost(url, request, CreateOrderResponse.class);
    }
    
    /**
     * Get QR code by matching ID
     */
    public QrCodeResponse getQrCode(String matchingId) {
        String url = config.getApiEndpoint() + "/esims/qr/" + matchingId;
        HttpHeaders headers = createHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );
        
        QrCodeResponse qrResponse = new QrCodeResponse();
        qrResponse.setQrCode(response.getBody());
        qrResponse.setMatchingId(matchingId);
        return qrResponse;
    }
    
    /**
     * Get order details by order ID
     */
    public OrderDetailResponse getOrderDetails(String orderId) {
        String url = config.getApiEndpoint() + "/orders/" + orderId;
        return executeGet(url, OrderDetailResponse.class);
    }
    
    private <T> T executeGet(String url, Class<T> responseType) {
        HttpHeaders headers = createHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        ResponseEntity<T> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                responseType
        );
        
        return response.getBody();
    }
    
    private <T> T executePost(String url, Object requestBody, Class<T> responseType) {
        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<T> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                responseType
        );
        
        return response.getBody();
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", config.getApiKey());
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }
}
