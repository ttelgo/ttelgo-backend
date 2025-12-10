package com.tiktel.ttelgo.faq.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.faq.api.dto.CreateFaqRequest;
import com.tiktel.ttelgo.faq.api.dto.FaqDto;
import com.tiktel.ttelgo.faq.api.dto.UpdateFaqRequest;
import com.tiktel.ttelgo.faq.application.FaqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize; // Temporarily disabled for development
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faq")
@RequiredArgsConstructor
public class FaqController {
    
    private final FaqService faqService;
    
    /**
     * Get all active FAQs (public endpoint)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FaqDto>>> getAllActiveFaqs(
            @RequestParam(required = false) String category
    ) {
        List<FaqDto> faqs;
        if (category != null && !category.trim().isEmpty()) {
            faqs = faqService.getFaqsByCategory(category.trim());
        } else {
            faqs = faqService.getAllActiveFaqs();
        }
        return ResponseEntity.ok(ApiResponse.success(faqs));
    }
    
    /**
     * Get all FAQ categories (public endpoint)
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        List<String> categories = faqService.getCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
    
    // ========== Admin Endpoints ==========
    
    /**
     * Get all FAQs including inactive (admin only)
     * Note: @PreAuthorize removed for development - add back in production with proper authentication
     */
    @GetMapping("/admin/all")
    // @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')") // Temporarily disabled for development
    public ResponseEntity<ApiResponse<List<FaqDto>>> getAllFaqsAdmin() {
        List<FaqDto> faqs = faqService.getAllFaqs();
        return ResponseEntity.ok(ApiResponse.success(faqs));
    }
    
    /**
     * Get FAQ by ID (admin only)
     * Note: @PreAuthorize removed for development - add back in production with proper authentication
     */
    @GetMapping("/admin/{id}")
    // @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')") // Temporarily disabled for development
    public ResponseEntity<ApiResponse<FaqDto>> getFaqById(@PathVariable Long id) {
        FaqDto faq = faqService.getFaqById(id);
        return ResponseEntity.ok(ApiResponse.success(faq));
    }
    
    /**
     * Create new FAQ (admin only)
     * Note: @PreAuthorize removed for development - add back in production with proper authentication
     */
    @PostMapping
    // @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')") // Temporarily disabled for development
    public ResponseEntity<ApiResponse<FaqDto>> createFaq(@Valid @RequestBody CreateFaqRequest request) {
        FaqDto faq = faqService.createFaq(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(faq));
    }
    
    /**
     * Update FAQ (admin only)
     * Note: @PreAuthorize removed for development - add back in production with proper authentication
     */
    @PutMapping("/{id}")
    // @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')") // Temporarily disabled for development
    public ResponseEntity<ApiResponse<FaqDto>> updateFaq(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFaqRequest request
    ) {
        FaqDto faq = faqService.updateFaq(id, request);
        return ResponseEntity.ok(ApiResponse.success(faq));
    }
    
    /**
     * Delete FAQ (admin only)
     * Note: @PreAuthorize removed for development - add back in production with proper authentication
     */
    @DeleteMapping("/{id}")
    // @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')") // Temporarily disabled for development
    public ResponseEntity<ApiResponse<Void>> deleteFaq(@PathVariable Long id) {
        faqService.deleteFaq(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

