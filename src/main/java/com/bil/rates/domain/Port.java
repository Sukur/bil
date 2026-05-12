package com.bil.rates.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "port")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Port {
    @Id
    @Column(length = 5)
    private String unloc;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 64)
    private String country;

    @Column(length = 64)
    private String region;

    private Double lat;
    private Double lon;
}

