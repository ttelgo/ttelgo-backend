package com.tiktel.ttelgo.apikey.api;

import com.tiktel.ttelgo.apikey.api.dto.*;
import com.tiktel.ttelgo.apikey.application.ApiKeyService;
import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.dto.PaginationMeta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/api-keys")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class ApiKeyController {
    
    private final ApiKeyService apiKeyService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<ApiKeyDto>> createApiKey(@Valid @RequestBody CreateApiKeyRequest request) {
        ApiKeyDto apiKey = apiKeyService.createApiKey(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(apiKey));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<ApiKeyDto>>> getAllApiKeys(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer size,
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<ApiKeyDto> apiKeys = apiKeyService.getApiKeys(pageable);
        return ResponseEntity.ok(ApiResponse.success(apiKeys.getContent(), "Success", PaginationMeta.fromPage(apiKeys)));
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
    public ResponseEntity<ApiResponse<Object>> deleteApiKey(@PathVariable Long id) {
        apiKeyService.deleteApiKey(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted", null));
    }
    
    @PostMapping("/{id}/regenerations")
    public ResponseEntity<ApiResponse<ApiKeyDto>> regenerateApiKey(@PathVariable Long id) {
        ApiKeyDto apiKey = apiKeyService.regenerateApiKey(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(apiKey));
    }
    
    @GetMapping("/{id}/usage")
    public ResponseEntity<ApiResponse<ApiUsageStatsDto>> getUsageStats(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        ApiUsageStatsDto stats = apiKeyService.getUsageStats(id, days);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sort.split(",", 2);
        String field = parts[0].trim();
        String dir = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";
        Sort.Direction direction = "desc".equals(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }
}

