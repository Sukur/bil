package com.bil.rates.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * A single carrier option returned by the /compare endpoint.
 */
public record QuoteOption(
        String tag,            // "CHEAPEST", "FASTEST", "BEST VALUE", ""
        String carrierScac,
        String carrierName,
        BigDecimal oceanFreight,
        BigDecimal markupAmount,
        BigDecimal totalAmount,
        String currency,
        Integer transitDays,
        Long rateLineId,
        List<String> tags      // all applicable tags for this option
) {}

