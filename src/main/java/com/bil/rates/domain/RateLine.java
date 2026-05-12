package com.bil.rates.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "rate_line", indexes = @Index(name = "ix_rateline_lookup", columnList = "pol,pod,equipment"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RateLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ratesheet_id", nullable = false)
    private Ratesheet ratesheet;

    @Column(nullable = false, length = 5)
    private String pol;

    @Column(nullable = false, length = 5)
    private String pod;

    @Column(length = 64)
    private String via;

    @Column(length = 64)
    private String service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Equipment equipment;

    @Column(length = 64)
    @Builder.Default
    private String commodity = "FAK";

    @Column(name = "base_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "transit_days")
    private Integer transitDays;

    @Column(name = "allocation_teu")
    private Integer allocationTeu;
}

