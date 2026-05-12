package com.bil.rates.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record QuoteResponse(
        Long quoteId,
        boolean found,
        String pol,
        String pod,
        String equipment,
        String commodity,
        LocalDate readyDate,
        String carrierScac,
        String carrierName,
        BigDecimal oceanFreight,
        BigDecimal markupAmount,
        BigDecimal totalAmount,
        String currency,
        LocalDate validUntil,
        Integer transitDays,
        Integer candidateCount,
        List<QuoteChargeDto> charges,
        Instant createdAt,
        String message
) {}
