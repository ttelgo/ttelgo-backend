package com.tiktel.ttelgo.faq.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateFaqRequest {
    @NotBlank(message = "Question is required")
    private String question;
    
    @NotBlank(message = "Answer is required")
    private String answer;
    
    private Integer displayOrder = 0;
    
    private Boolean isActive = true;
    
    private String category;
}

