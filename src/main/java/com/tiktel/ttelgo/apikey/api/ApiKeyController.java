package com.tiktel.ttelgo.apikey.api;

import com.tiktel.ttelgo.apikey.api.dto.*;
import com.tiktel.ttelgo.apikey.application.ApiKeyService;
import com.tiktel.ttelgo.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {
    
    private final ApiKeyService apiKeyService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<ApiKeyDto>> createApiKey(@Valid @RequestBody CreateApiKeyRequest request) {
        ApiKeyDto apiKey = apiKeyService.createApiKey(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(apiKey));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<ApiKeyDto>>> getAllApiKeys() {
        List<ApiKeyDto> apiKeys = apiKeyService.getAllApiKeys();
        return ResponseEntity.ok(ApiResponse.success(apiKeys));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApiKeyDto>> getApiKeyById(@PathVariable Long id) {
        ApiKeyDto apiKey = apiKeyService.getApiKeyById(id);
        return ResponseEntity.ok(ApiResponse.success(apiKey));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ApiKeyDto>> updateApiKey(
            @PathVariable Long id,
            @Valid @RequestBody UpdateApiKeyRequest request) {
        ApiKeyDto apiKey = apiKeyService.updateApiKey(id, request);
        return ResponseEntity.ok(ApiResponse.success(apiKey));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteApiKey(@PathVariable Long id) {
        apiKeyService.deleteApiKey(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    @PostMapping("/{id}/regenerate")
    public ResponseEntity<ApiResponse<ApiKeyDto>> regenerateApiKey(@PathVariable Long id) {
        ApiKeyDto apiKey = apiKeyService.regenerateApiKey(id);
        return ResponseEntity.ok(ApiResponse.success(apiKey));
    }
    
    @GetMapping("/{id}/usage")
    public ResponseEntity<ApiResponse<ApiUsageStatsDto>> getUsageStats(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        ApiUsageStatsDto stats = apiKeyService.getUsageStats(id, days);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}

