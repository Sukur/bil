package com.bil.rates.api.dto;

import com.bil.rates.domain.Equipment;

import java.math.BigDecimal;

/** Single rate line as exposed via the public API. */
public record RateLineDto(
        Long id,
        String pol,
        String pod,
        String via,
        String service,
        Equipment equipment,
        String commodity,
        BigDecimal baseAmount,
        String currency,
        Integer transitDays
) {}

