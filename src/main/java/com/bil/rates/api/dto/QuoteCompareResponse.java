package com.bil.rates.api.dto;

import java.util.List;

/**
 * Response for POST /api/v1/quotes/compare — multiple carrier options for one route.
 */
public record QuoteCompareResponse(
        String pol,
        String pod,
        String equipment,
        String commodity,
        String readyDate,
        int totalCandidates,
        List<QuoteOption> options
) {}

