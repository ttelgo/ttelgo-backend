package com.tiktel.ttelgo.auth.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sessions")
@Data
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // TODO: Implement session domain entity
}

