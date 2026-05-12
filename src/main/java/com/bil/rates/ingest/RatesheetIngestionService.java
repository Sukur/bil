package com.bil.rates.ingest;

import org.springframework.stereotype.Service;

/**
 * Kept for backward-compatibility wiring only.
 * All business logic lives in {@link com.bil.rates.service.RatesheetService}.
 * @deprecated Use {@code RatesheetService} instead.
 */
@Deprecated(forRemoval = true)
@Service
public class RatesheetIngestionService {
    // Intentionally empty — superseded by RatesheetService
}


