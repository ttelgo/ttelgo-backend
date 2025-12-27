package com.tiktel.ttelgo.kyc.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycResponse {
    private Long id;
    private Long userId;
    private String status;
    private String documentType;
    private String documentNumber;
    private String documentFrontUrl;
    private String documentBackUrl;
    private String selfieUrl;
    private String firstName;
    private String lastName;
    private LocalDateTime dateOfBirth;
    private String nationality;
    private String address;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

