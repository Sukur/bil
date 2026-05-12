package com.bil.rates.ingest;

import com.bil.rates.domain.Equipment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** Canonical rate row prior to persistence. */
@Data
@Builder
public class CanonicalRateLineDto {
    private String pol;          // UN/LOCODE
    private String polName;
    private String pod;
    private String podName;
    private String via;          // transhipment port(s)
    private Equipment equipment;
    private String commodity;    // FAK by default
    private BigDecimal baseAmount;
    private String currency;     // ISO-4217
    private Integer transitDays;
}

