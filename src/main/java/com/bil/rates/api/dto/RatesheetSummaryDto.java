package com.bil.rates.api.dto;

import com.bil.rates.domain.RatesheetStatus;
import com.bil.rates.domain.RatesheetType;

import java.time.Instant;
import java.time.LocalDate;

/** Compact representation for ratesheet listing pages. */
public record RatesheetSummaryDto(
        Long id,
        String carrierScac,
        String carrierName,
        String source,
        String sourceFile,
        RatesheetType type,
        String currency,
        LocalDate validFrom,
        LocalDate validTo,
        RatesheetStatus status,
        String version,
        Instant uploadedAt,
        long lineCount
) {}

