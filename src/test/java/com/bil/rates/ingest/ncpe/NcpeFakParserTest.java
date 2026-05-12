package com.bil.rates.ingest.ncpe;

import com.bil.rates.domain.Equipment;
import com.bil.rates.ingest.CanonicalRatesheetDto;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class NcpeFakParserTest {

    /** Path to the actual NCPE ratesheet under docs/. */
    private static final Path NCPE_FILE = Path.of(
            "docs/NCPE - 2026.04.01-2026.05.31 - DRY FAK DE.NL.BE Tariff (V.2026.03.04).xlsx");

    static boolean ncpeFilePresent() {
        return Files.exists(NCPE_FILE);
    }

    @Test
    @EnabledIf("ncpeFilePresent")
    void parsesNcpeRatesheetWithRealFile() throws Exception {
        NcpeFakParser parser = new NcpeFakParser();

        try (InputStream in = Files.newInputStream(NCPE_FILE);
             Workbook wb = WorkbookFactory.create(in)) {

            assertThat(parser.supports(NCPE_FILE.getFileName().toString(), wb)).isTrue();

            CanonicalRatesheetDto dto = parser.parse(NCPE_FILE.getFileName().toString(), wb);

            assertThat(dto.getCarrierScac()).isEqualTo("NCPE");
            assertThat(dto.getCurrency()).isEqualTo("USD");
            assertThat(dto.getValidFrom()).isEqualTo(java.time.LocalDate.of(2026, 4, 1));
            assertThat(dto.getValidTo()).isEqualTo(java.time.LocalDate.of(2026, 5, 31));
            assertThat(dto.getLines()).isNotEmpty();

            // Each row produces up to 3 lines (20DC, 40DC, 40HC). Spot-check a few:
            assertThat(dto.getLines())
                    .allSatisfy(l -> {
                        assertThat(l.getPol()).hasSize(5);
                        assertThat(l.getPod()).hasSize(5);
                        assertThat(l.getEquipment()).isIn(Equipment.DC20, Equipment.DC40, Equipment.HC40);
                        assertThat(l.getBaseAmount().signum()).isPositive();
                    });

            // Known sample from the introspection: BEANR -> AUBNE, 20DC = 1600
            boolean foundSample = dto.getLines().stream().anyMatch(l ->
                    "BEANR".equals(l.getPol()) && "AUBNE".equals(l.getPod())
                    && l.getEquipment() == Equipment.DC20
                    && l.getBaseAmount().intValueExact() == 1600);
            assertThat(foundSample)
                    .as("Expected BEANR→AUBNE 20DC=1600 in parsed lines")
                    .isTrue();
        }
    }

    @Test
    void validityRegexHandlesDottedFormat() {
        java.time.LocalDate[] r = NcpeFakParser.parseValidity("Validity: 2026.04.01-2026.05.31 (Based on LTS)");
        assertThat(r[0]).isEqualTo(java.time.LocalDate.of(2026, 4, 1));
        assertThat(r[1]).isEqualTo(java.time.LocalDate.of(2026, 5, 31));
    }
}

