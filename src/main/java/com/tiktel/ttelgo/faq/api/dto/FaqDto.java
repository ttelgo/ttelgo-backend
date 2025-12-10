package com.tiktel.ttelgo.faq.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaqDto {
    private Long id;
    private String question;
    private String answer;
    private Integer displayOrder;
    private Boolean isActive;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

