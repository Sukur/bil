package com.bil.rates.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "carrier")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Carrier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String scac;

    @Column(nullable = false, length = 128)
    private String name;
}

