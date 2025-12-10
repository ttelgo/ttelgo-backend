package com.tiktel.ttelgo.kyc.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_verifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Kyc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private KycStatus status = KycStatus.PENDING;
    
    @Column(name = "document_type")
    private String documentType; // passport, id_card, driver_license
    
    @Column(name = "document_number")
    private String documentNumber;
    
    @Column(name = "document_front_url", columnDefinition = "TEXT")
    private String documentFrontUrl;
    
    @Column(name = "document_back_url", columnDefinition = "TEXT")
    private String documentBackUrl;
    
    @Column(name = "selfie_url", columnDefinition = "TEXT")
    private String selfieUrl;
    
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    @Column(name = "date_of_birth")
    private LocalDateTime dateOfBirth;
    
    @Column(name = "nationality")
    private String nationality;
    
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    @Column(name = "reviewed_by")
    private Long reviewedBy; // Admin user ID
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

