package com.tiktel.ttelgo.faq.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.dto.PaginationMeta;
import com.tiktel.ttelgo.faq.api.dto.CreateFaqRequest;
import com.tiktel.ttelgo.faq.api.dto.FaqDto;
import com.tiktel.ttelgo.faq.api.dto.UpdateFaqRequest;
import com.tiktel.ttelgo.faq.application.FaqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin FAQ management as a RESTful resource.
 */
@RestController
@RequestMapping("/api/v1/admin/faqs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminFaqController {

    private final FaqService faqService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FaqDto>>> listAllFaqs(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer size,
            @RequestParam(required = false, defaultValue = "displayOrder,asc") String sort
    ) {
        List<FaqDto> all = faqService.getAllFaqs();
        List<FaqDto> sorted = applySort(all, sort);
        List<FaqDto> paged = slice(sorted, page, size);
        return ResponseEntity.ok(ApiResponse.success(paged, "Success", PaginationMeta.simple(page, size, sorted.size())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FaqDto>> getFaq(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(faqService.getFaqById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FaqDto>> createFaq(@Valid @RequestBody CreateFaqRequest request) {
        FaqDto faq = faqService.createFaq(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(faq));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FaqDto>> updateFaq(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFaqRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(faqService.updateFaq(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteFaq(@PathVariable Long id) {
        faqService.deleteFaq(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted", null));
    }

    private List<FaqDto> slice(List<FaqDto> items, int page, int size) {
        if (items == null || items.isEmpty()) return items;
        if (size <= 0) return items;
        int from = Math.max(0, page) * size;
        if (from >= items.size()) return List.of();
        int to = Math.min(items.size(), from + size);
        return items.subList(from, to);
    }

    private List<FaqDto> applySort(List<FaqDto> items, String sort) {
        if (items == null) return List.of();
        Comparator<FaqDto> comparator = Comparator.comparing(FaqDto::getDisplayOrder, Comparator.nullsLast(Integer::compareTo));

        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", 2);
            String field = parts[0].trim();
            String dir = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";
            boolean desc = "desc".equals(dir);

            switch (field) {
                case "question" -> comparator = Comparator.comparing(FaqDto::getQuestion, Comparator.nullsLast(String::compareToIgnoreCase));
                case "category" -> comparator = Comparator.comparing(FaqDto::getCategory, Comparator.nullsLast(String::compareToIgnoreCase));
                case "displayOrder" -> comparator = Comparator.comparing(FaqDto::getDisplayOrder, Comparator.nullsLast(Integer::compareTo));
                default -> {
                }
            }
            if (desc) comparator = comparator.reversed();
        }

        return items.stream().sorted(comparator).collect(Collectors.toList());
    }
}


