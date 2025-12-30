package com.tiktel.ttelgo.auth.infrastructure.repository;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "otp_tokens")
@Data
public class OtpTokenJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // TODO: Implement OTP token JPA entity
}

