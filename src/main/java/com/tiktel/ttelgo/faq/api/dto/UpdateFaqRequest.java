package com.tiktel.ttelgo.faq.api.dto;

import lombok.Data;

@Data
public class UpdateFaqRequest {
    private String question;
    private String answer;
    private Integer displayOrder;
    private Boolean isActive;
    private String category;
}

