package com.tiktel.ttelgo.auth.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "otp_tokens")
@Data
public class OtpToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // TODO: Implement OTP token domain entity
}

