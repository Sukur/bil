package com.bil.rates.manager;

import com.bil.rates.api.dto.RateLineDto;
import com.bil.rates.api.dto.RatesheetSummaryDto;
import com.bil.rates.domain.RateLine;
import com.bil.rates.domain.Ratesheet;

/**
 * Hand-written entity ↔ DTO conversions.
 * Centralised so business logic in services never touches DTO construction directly.
 */
public final class DtoAssembler {

    private DtoAssembler() {}

    public static RatesheetSummaryDto toSummary(Ratesheet rs, long lineCount) {
        return new RatesheetSummaryDto(
                rs.getId(),
                rs.getCarrier() != null ? rs.getCarrier().getScac() : null,
                rs.getCarrier() != null ? rs.getCarrier().getName() : null,
                rs.getSource(),
                rs.getSourceFile(),
                rs.getType(),
                rs.getCurrency(),
                rs.getValidFrom(),
                rs.getValidTo(),
                rs.getStatus(),
                rs.getVersion(),
                rs.getUploadedAt(),
                lineCount
        );
    }

    public static RateLineDto toLineDto(RateLine l) {
        return new RateLineDto(
                l.getId(),
                l.getPol(),
                l.getPod(),
                l.getVia(),
                l.getService(),
                l.getEquipment(),
                l.getCommodity(),
                l.getBaseAmount(),
                l.getCurrency(),
                l.getTransitDays()
        );
    }
}

