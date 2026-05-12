package com.bil.rates.api.dto;

/** Result of an Excel import request. */
public record ImportResultDto(
        Long ratesheetId,
        String parser,
        int linesImported,
        String message
) {}

