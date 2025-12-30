package com.tiktel.ttelgo.faq.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.dto.PaginationMeta;
import com.tiktel.ttelgo.faq.api.dto.FaqDto;
import com.tiktel.ttelgo.faq.application.FaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize; // Temporarily disabled for development
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/faqs")
@RequiredArgsConstructor
public class FaqController {
    
    private final FaqService faqService;
    
    /**
     * Get all active FAQs (public endpoint)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FaqDto>>> getAllActiveFaqs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer size,
            @RequestParam(required = false, defaultValue = "displayOrder,asc") String sort
    ) {
        List<FaqDto> faqs;
        if (category != null && !category.trim().isEmpty()) {
            faqs = faqService.getFaqsByCategory(category.trim());
        } else {
            faqs = faqService.getAllActiveFaqs();
        }

        List<FaqDto> sorted = applySort(faqs, sort);
        List<FaqDto> paged = slice(sorted, page, size);
        return ResponseEntity.ok(ApiResponse.success(paged, "Success", PaginationMeta.simple(page, size, sorted.size())));
    }
    
    /**
     * Get all FAQ categories (public endpoint)
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        List<String> categories = faqService.getCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
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

