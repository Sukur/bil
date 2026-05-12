package com.bil.rates.api.dto;

import java.time.LocalDate;

public record QuoteRequest(
        String pol,
        String pod,
        String equipment,
        String commodity,
        LocalDate readyDate,
        Integer weightKg
) {}
