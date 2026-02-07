package com.tiktel.ttelgo.admin.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.dto.PaginationMeta;
import com.tiktel.ttelgo.common.domain.enums.EsimStatus;
import com.tiktel.ttelgo.esim.domain.Esim;
import com.tiktel.ttelgo.esim.infrastructure.mapper.EsimMapper;
import com.tiktel.ttelgo.esim.infrastructure.repository.EsimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    private final EsimMapper esimMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllEsims(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));

        Page<Esim> esims;
        if (status != null && !status.isEmpty()) {
            try {
                EsimStatus esimStatus = EsimStatus.valueOf(status.toUpperCase());
                esims = esimRepository.findByStatus(esimStatus, pageable).map(esimMapper::toDomain);
            } catch (IllegalArgumentException e) {
                esims = esimRepository.findAll(pageable).map(esimMapper::toDomain);
            }
        } else {
            esims = esimRepository.findAll(pageable).map(esimMapper::toDomain);
        }

        List<Map<String, Object>> esimResponses = esims.getContent().stream()
            .map(esim -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", esim.getId());
                map.put("esimUuid", esim.getMatchingId());
                map.put("orderId", esim.getOrderId());
                map.put("userId", esim.getUserId());
                map.put("bundleId", esim.getBundleCode());
                map.put("bundleName", esim.getBundleName());
                map.put("matchingId", esim.getMatchingId());
                map.put("iccid", esim.getIccid());
                map.put("smdpAddress", esim.getSmdpAddress());
                map.put("status", esim.getStatus() != null ? esim.getStatus().name() : null);
                map.put("activatedAt", esim.getActivatedAt());
                map.put("expiresAt", esim.getValidUntil());
                map.put("createdAt", esim.getCreatedAt());
                map.put("updatedAt", esim.getUpdatedAt());
                return map;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(esimResponses, "Success", PaginationMeta.fromPage(esims)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEsimById(@PathVariable Long id) {
        Esim esim = esimRepository.findById(id)
            .map(esimMapper::toDomain)
            .orElseThrow(() -> new RuntimeException("eSIM not found"));

        Map<String, Object> map = new HashMap<>();
        map.put("id", esim.getId());
        map.put("esimUuid", esim.getMatchingId());
        map.put("orderId", esim.getOrderId());
        map.put("userId", esim.getUserId());
        map.put("bundleId", esim.getBundleCode());
        map.put("bundleName", esim.getBundleName());
        map.put("matchingId", esim.getMatchingId());
        map.put("iccid", esim.getIccid());
        map.put("smdpAddress", esim.getSmdpAddress());
        map.put("status", esim.getStatus() != null ? esim.getStatus().name() : null);
        map.put("activatedAt", esim.getActivatedAt());
        map.put("expiresAt", esim.getValidUntil());
        map.put("createdAt", esim.getCreatedAt());
        map.put("updatedAt", esim.getUpdatedAt());

        return ResponseEntity.ok(ApiResponse.success(map));
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
