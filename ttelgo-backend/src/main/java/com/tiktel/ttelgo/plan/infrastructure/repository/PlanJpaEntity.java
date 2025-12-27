package com.tiktel.ttelgo.plan.infrastructure.repository;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "plans")
@Data
public class PlanJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // TODO: Implement plan JPA entity
}

