package com.bil.rates.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an instant ocean-freight quotation.
 *
 * MVP scope: single ocean leg, 10 % hardcoded markup, no surcharges.
 */
@Entity
@Table(name = "quote",
        indexes = @Index(name = "ix_quote_created", columnList = "created_at"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", length = 64)
    @Builder.Default
    private String tenantId = "default";

    /** Port of Loading (UN/LOCODE). */
    @Column(nullable = false, length = 5)
    private String pol;

    /** Port of Discharge (UN/LOCODE). */
    @Column(nullable = false, length = 5)
    private String pod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Equipment equipment;

    @Column(length = 64)
    @Builder.Default
    private String commodity = "FAK";

    @Column(name = "ready_date")
    private LocalDate readyDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private QuoteStatus status = QuoteStatus.OPEN;

    /** Expiry of this quote offer. */
    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "carrier_scac", length = 8)
    private String carrierScac;

    @Column(name = "carrier_name", length = 128)
    private String carrierName;

    /** Reference to the RateLine this quote is priced from. */
    @Column(name = "rate_line_id")
    private Long rateLineId;

    @Column(name = "ratesheet_id")
    private Long ratesheetId;

    /** Base ocean freight (from the rate line). */
    @Column(name = "ocean_freight", precision = 12, scale = 2)
    private BigDecimal oceanFreight;

    /** Markup applied (10 % MVP). */
    @Column(name = "markup_amount", precision = 12, scale = 2)
    private BigDecimal markupAmount;

    /** Net total (oceanFreight + markupAmount). */
    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /** Currency inherited from the winning rate line. */
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "transit_days")
    private Integer transitDays;

    @Column(name = "weight_kg")
    private Integer weightKg;

    @Column(name = "candidate_count")
    private Integer candidateCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuoteCharge> charges = new ArrayList<>();
}

