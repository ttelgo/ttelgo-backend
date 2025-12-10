package com.tiktel.ttelgo.admin.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.esim.domain.Esim;
import com.tiktel.ttelgo.esim.infrastructure.repository.EsimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/esims")
@RequiredArgsConstructor
public class AdminEsimController {
    
    private final EsimRepository esimRepository;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllEsims(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer size,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<Esim> esims;
        if (status != null && !status.isEmpty()) {
            try {
                com.tiktel.ttelgo.esim.domain.EsimStatus esimStatus = 
                    com.tiktel.ttelgo.esim.domain.EsimStatus.valueOf(status.toUpperCase());
                esims = esimRepository.findByStatus(esimStatus, pageable);
            } catch (IllegalArgumentException e) {
                esims = esimRepository.findAll(pageable);
            }
        } else {
            esims = esimRepository.findAll(pageable);
        }
        
        List<Map<String, Object>> esimResponses = esims.getContent().stream()
            .map(esim -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", esim.getId());
                map.put("esimUuid", esim.getEsimUuid());
                map.put("orderId", esim.getOrderId());
                map.put("userId", esim.getUserId());
                map.put("bundleId", esim.getBundleId());
                map.put("bundleName", esim.getBundleName());
                map.put("matchingId", esim.getMatchingId());
                map.put("iccid", esim.getIccid());
                map.put("smdpAddress", esim.getSmdpAddress());
                map.put("status", esim.getStatus() != null ? esim.getStatus().name() : null);
                map.put("activatedAt", esim.getActivatedAt());
                map.put("expiresAt", esim.getExpiresAt());
                map.put("createdAt", esim.getCreatedAt());
                map.put("updatedAt", esim.getUpdatedAt());
                return map;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(esimResponses));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEsimById(@PathVariable Long id) {
        Esim esim = esimRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("eSIM not found"));
        
        Map<String, Object> map = new HashMap<>();
        map.put("id", esim.getId());
        map.put("esimUuid", esim.getEsimUuid());
        map.put("orderId", esim.getOrderId());
        map.put("userId", esim.getUserId());
        map.put("bundleId", esim.getBundleId());
        map.put("bundleName", esim.getBundleName());
        map.put("matchingId", esim.getMatchingId());
        map.put("iccid", esim.getIccid());
        map.put("smdpAddress", esim.getSmdpAddress());
        map.put("status", esim.getStatus() != null ? esim.getStatus().name() : null);
        map.put("activatedAt", esim.getActivatedAt());
        map.put("expiresAt", esim.getExpiresAt());
        map.put("createdAt", esim.getCreatedAt());
        map.put("updatedAt", esim.getUpdatedAt());
        
        return ResponseEntity.ok(ApiResponse.success(map));
    }
}

