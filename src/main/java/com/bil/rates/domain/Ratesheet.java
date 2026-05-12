package com.bil.rates.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "ratesheet", indexes = @Index(name = "ix_ratesheet_validity", columnList = "valid_from,valid_to"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ratesheet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carrier_id", nullable = false)
    private Carrier carrier;

    /** Logical source identifier, e.g. "NCPE_DRY_FAK_DE_NL_BE". */
    @Column(nullable = false, length = 64)
    private String source;

    @Column(name = "source_file", length = 512)
    private String sourceFile;

    @Column(name = "contract_no", length = 64)
    private String contractNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RatesheetType type;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RatesheetStatus status;

    @Column(length = 32)
    private String version;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "tenant_id", length = 64)
    private String tenantId;
}

