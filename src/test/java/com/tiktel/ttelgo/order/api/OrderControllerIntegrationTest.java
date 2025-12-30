package com.tiktel.ttelgo.order.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiktel.ttelgo.order.api.dto.CreateOrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OrderController
 * Note: Requires test database and Spring context
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testCreateOrder_WithoutAuth_Returns401() throws Exception {
        // Arrange
        CreateOrderRequest request = CreateOrderRequest.builder()
                .bundleCode("BUNDLE_US_5GB_30D")
                .quantity(1)
                .customerEmail("test@example.com")
                .build();
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testCreateOrder_InvalidRequest_Returns400() throws Exception {
        // Arrange - missing required fields
        CreateOrderRequest request = CreateOrderRequest.builder()
                .quantity(1)
                .build();
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

