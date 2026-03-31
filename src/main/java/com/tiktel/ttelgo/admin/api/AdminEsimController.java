package com.tiktel.ttelgo.admin.api;

import com.tiktel.ttelgo.common.domain.enums.EsimStatus;
import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.dto.PaginationMeta;
import com.tiktel.ttelgo.esim.infrastructure.repository.EsimJpaEntity;
import com.tiktel.ttelgo.esim.infrastructure.repository.EsimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/esims")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminEsimController {
    
    private final EsimRepository esimRepository;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllEsims(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        
        Page<EsimJpaEntity> esims;
        if (status != null && !status.isEmpty()) {
            try {
                com.tiktel.ttelgo.common.domain.enums.EsimStatus esimStatus = 
                    com.tiktel.ttelgo.common.domain.enums.EsimStatus.valueOf(status.toUpperCase());
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
                map.put("orderId", esim.getOrderId());
                map.put("userId", esim.getUserId());
                map.put("bundleCode", esim.getBundleCode());
                map.put("bundleName", esim.getBundleName());
                map.put("matchingId", esim.getMatchingId());
                map.put("iccid", esim.getIccid());
                map.put("smdpAddress", esim.getSmdpAddress());
                map.put("status", esim.getStatus() != null ? esim.getStatus().name() : null);
                map.put("activatedAt", esim.getActivatedAt());
                map.put("expiredAt", esim.getExpiredAt());
                map.put("createdAt", esim.getCreatedAt());
                map.put("updatedAt", esim.getUpdatedAt());
                return map;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(esimResponses, "Success", PaginationMeta.fromPage(esims)));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEsimById(@PathVariable Long id) {
        EsimJpaEntity esim = esimRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("eSIM not found"));
        
        Map<String, Object> map = new HashMap<>();
        map.put("id", esim.getId());
        map.put("orderId", esim.getOrderId());
        map.put("userId", esim.getUserId());
        map.put("bundleCode", esim.getBundleCode());
        map.put("bundleName", esim.getBundleName());
        map.put("matchingId", esim.getMatchingId());
        map.put("iccid", esim.getIccid());
        map.put("smdpAddress", esim.getSmdpAddress());
        map.put("status", esim.getStatus() != null ? esim.getStatus().name() : null);
        map.put("activatedAt", esim.getActivatedAt());
        map.put("expiredAt", esim.getExpiredAt());
        map.put("createdAt", esim.getCreatedAt());
        map.put("updatedAt", esim.getUpdatedAt());
        
        return ResponseEntity.ok(ApiResponse.success(map));
    }

    /**
     * Update eSIM status (and optionally validUntil / expiredAt).
     * PATCH /api/v1/admin/esims/{id}
     * Body: { "status": "EXPIRED", "validUntil": "2020-01-01T00:00:00" }
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateEsim(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {

        EsimJpaEntity esim = esimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("eSIM not found: " + id));

        // Update status
        if (updates.containsKey("status")) {
            try {
                EsimStatus newStatus = EsimStatus.valueOf(updates.get("status").toString().toUpperCase());
                esim.setStatus(newStatus);
                if (newStatus == EsimStatus.EXPIRED && esim.getExpiredAt() == null) {
                    esim.setExpiredAt(LocalDateTime.now());
                }
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid status value: " + updates.get("status")));
            }
        }

        // Update validUntil (accepts ISO LocalDateTime string)
        if (updates.containsKey("validUntil") && updates.get("validUntil") != null) {
            try {
                LocalDateTime validUntil = LocalDateTime.parse(updates.get("validUntil").toString());
                esim.setValidUntil(validUntil);
                // If date is in the past and status not already set, mark as expired
                if (validUntil.isBefore(LocalDateTime.now()) && esim.getStatus() != EsimStatus.EXPIRED) {
                    esim.setStatus(EsimStatus.EXPIRED);
                    esim.setExpiredAt(validUntil);
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid validUntil format. Use ISO format: 2020-01-01T00:00:00"));
            }
        }

        // Update expiredAt explicitly if provided
        if (updates.containsKey("expiredAt") && updates.get("expiredAt") != null) {
            try {
                esim.setExpiredAt(LocalDateTime.parse(updates.get("expiredAt").toString()));
            } catch (Exception e) {
                // ignore, non-critical
            }
        }

        esim.setUpdatedAt(LocalDateTime.now());
        EsimJpaEntity saved = esimRepository.save(esim);

        Map<String, Object> result = new HashMap<>();
        result.put("id", saved.getId());
        result.put("orderId", saved.getOrderId());
        result.put("userId", saved.getUserId());
        result.put("status", saved.getStatus() != null ? saved.getStatus().name() : null);
        result.put("validUntil", saved.getValidUntil());
        result.put("expiredAt", saved.getExpiredAt());
        result.put("updatedAt", saved.getUpdatedAt());

        return ResponseEntity.ok(ApiResponse.success(result, "eSIM updated successfully"));
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

