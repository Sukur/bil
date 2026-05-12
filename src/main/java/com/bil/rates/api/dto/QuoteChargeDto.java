package com.bil.rates.api.dto;

import java.math.BigDecimal;

public record QuoteChargeDto(
        String code,
        String description,
        BigDecimal amount,
        String currency,
        String basis
) {}
