package com.tiktel.ttelgo.kyc.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "kyc_verifications")
@Data
public class Kyc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // TODO: Implement KYC domain entity
}

