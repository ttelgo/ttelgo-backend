package com.tiktel.ttelgo.kyc.infrastructure.repository;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "kyc_verifications")
@Data
public class KycJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // TODO: Implement KYC JPA entity
}

