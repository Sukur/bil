package com.bil.rates.ingest;

import org.apache.poi.ss.usermodel.Workbook;

/**
 * Adapter for translating a carrier-specific Excel ratesheet into the canonical
 * {@link CanonicalRatesheetDto} representation used by the ingestion pipeline.
 */
public interface RatesheetParser {

    /** Logical parser identifier (used for audit + dispatch). */
    String name();

    /** Cheap pre-flight check whether this parser can handle the workbook/filename. */
    boolean supports(String fileName, Workbook workbook);

    /** Parse the workbook into a canonical DTO. */
    CanonicalRatesheetDto parse(String fileName, Workbook workbook);
}

