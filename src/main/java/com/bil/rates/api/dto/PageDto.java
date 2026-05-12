package com.bil.rates.api.dto;

import java.util.List;

/** Generic page envelope to keep frontend simple. */
public record PageDto<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {}

