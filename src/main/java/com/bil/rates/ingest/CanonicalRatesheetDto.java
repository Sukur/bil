package com.bil.rates.ingest;

import com.bil.rates.domain.RatesheetType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** Canonical ratesheet representation prior to persistence. */
@Data
@Builder
public class CanonicalRatesheetDto {
    private String carrierScac;
    private String carrierName;
    private String source;          // logical key, e.g. NCPE_DRY_FAK_DE_NL_BE
    private String sourceFile;
    private String contractNo;
    private RatesheetType type;
    private String currency;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String version;
    private List<CanonicalRateLineDto> lines;
}

