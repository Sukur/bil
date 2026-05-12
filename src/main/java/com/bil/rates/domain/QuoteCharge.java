package com.bil.rates.domain;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity
@Table(name = "quote_charge")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuoteCharge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;
    @Column(nullable = false, length = 32)
    private String code;
    @Column(length = 128)
    private String description;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;
    /** PER_CNTR | PER_BL | PER_TEU | PERCENT */
    @Column(length = 32)
    private String basis;
}
