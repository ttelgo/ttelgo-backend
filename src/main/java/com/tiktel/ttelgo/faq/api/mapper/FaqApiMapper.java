package com.tiktel.ttelgo.faq.api.mapper;

import com.tiktel.ttelgo.faq.api.dto.CreateFaqRequest;
import com.tiktel.ttelgo.faq.api.dto.FaqDto;
import com.tiktel.ttelgo.faq.api.dto.UpdateFaqRequest;
import com.tiktel.ttelgo.faq.domain.Faq;

import java.util.List;
import java.util.stream.Collectors;

public class FaqApiMapper {
    
    public static FaqDto toDto(Faq faq) {
        if (faq == null) {
            return null;
        }
        
        return FaqDto.builder()
                .id(faq.getId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .displayOrder(faq.getDisplayOrder())
                .isActive(faq.getIsActive())
                .category(faq.getCategory())
                .createdAt(faq.getCreatedAt())
                .updatedAt(faq.getUpdatedAt())
                .build();
    }
    
    public static Faq toEntity(CreateFaqRequest request) {
        if (request == null) {
            return null;
        }
        
        return Faq.builder()
                .question(request.getQuestion())
                .answer(request.getAnswer())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .category(request.getCategory())
                .build();
    }
    
    public static void updateEntity(Faq faq, UpdateFaqRequest request) {
        if (request == null || faq == null) {
            return;
        }
        
        if (request.getQuestion() != null) {
            faq.setQuestion(request.getQuestion());
        }
        if (request.getAnswer() != null) {
            faq.setAnswer(request.getAnswer());
        }
        if (request.getDisplayOrder() != null) {
            faq.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsActive() != null) {
            faq.setIsActive(request.getIsActive());
        }
        if (request.getCategory() != null) {
            faq.setCategory(request.getCategory());
        }
    }
    
    public static List<FaqDto> toDtoList(List<Faq> faqs) {
        if (faqs == null) {
            return List.of();
        }
        return faqs.stream()
                .map(FaqApiMapper::toDto)
                .collect(Collectors.toList());
    }
}

