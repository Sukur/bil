package com.bil.rates.api.dto;

/** Aggregated statistics for the dashboard. */
public record DashboardStatsDto(
        long carrierCount,
        long activeRatesheets,
        long totalRatesheets,
        long totalRateLines,
        long portCount
) {}

