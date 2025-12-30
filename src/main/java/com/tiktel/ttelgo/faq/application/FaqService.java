package com.tiktel.ttelgo.faq.application;

import com.tiktel.ttelgo.faq.api.dto.CreateFaqRequest;
import com.tiktel.ttelgo.faq.api.dto.FaqDto;
import com.tiktel.ttelgo.faq.api.dto.UpdateFaqRequest;
import com.tiktel.ttelgo.faq.api.mapper.FaqApiMapper;
import com.tiktel.ttelgo.faq.domain.Faq;
import com.tiktel.ttelgo.faq.infrastructure.repository.FaqRepository;
import com.tiktel.ttelgo.common.exception.ErrorCode;
import com.tiktel.ttelgo.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FaqService {
    
    private final FaqRepository faqRepository;
    
    @Transactional(readOnly = true)
    public List<FaqDto> getAllActiveFaqs() {
        List<Faq> faqs = faqRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return FaqApiMapper.toDtoList(faqs);
    }
    
    @Transactional(readOnly = true)
    public List<FaqDto> getFaqsByCategory(String category) {
        List<Faq> faqs = faqRepository.findByIsActiveTrueAndCategoryOrderByDisplayOrderAsc(category);
        return FaqApiMapper.toDtoList(faqs);
    }
    
    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return faqRepository.findDistinctCategories();
    }
    
    @Transactional
    public FaqDto createFaq(CreateFaqRequest request) {
        Faq faq = FaqApiMapper.toEntity(request);
        Faq savedFaq = faqRepository.save(faq);
        return FaqApiMapper.toDto(savedFaq);
    }
    
    @Transactional
    public FaqDto updateFaq(Long id, UpdateFaqRequest request) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "FAQ not found with id: " + id));
        
        FaqApiMapper.updateEntity(faq, request);
        Faq updatedFaq = faqRepository.save(faq);
        return FaqApiMapper.toDto(updatedFaq);
    }
    
    @Transactional
    public void deleteFaq(Long id) {
        if (!faqRepository.existsById(id)) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "FAQ not found with id: " + id);
        }
        faqRepository.deleteById(id);
    }
    
    // Admin methods - get all FAQs including inactive
    @Transactional(readOnly = true)
    public List<FaqDto> getAllFaqs() {
        List<Faq> faqs = faqRepository.findAll();
        return FaqApiMapper.toDtoList(faqs);
    }
    
    @Transactional(readOnly = true)
    public FaqDto getFaqById(Long id) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "FAQ not found with id: " + id));
        return FaqApiMapper.toDto(faq);
    }
}

