package com.tiktel.ttelgo.esim.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "esims")
@Data
public class Esim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // TODO: Implement eSIM domain entity
}

