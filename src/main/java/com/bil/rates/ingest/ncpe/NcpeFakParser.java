package com.bil.rates.ingest.ncpe;

import com.bil.rates.domain.Equipment;
import com.bil.rates.domain.RatesheetType;
import com.bil.rates.ingest.CanonicalRateLineDto;
import com.bil.rates.ingest.CanonicalRatesheetDto;
import com.bil.rates.ingest.CellReader;
import com.bil.rates.ingest.RatesheetParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for NCPE DRY FAK DE.NL.BE Tariff ratesheets.
 *
 * Layout (per docs/_introspection):
 *   Row 1: "NCP Tariff" | "Update: dd.MM.yyyy"
 *   Row 2: Pay term
 *   Row 3: Surcharge inclusion notes (BF/CF/AG/...)
 *   Row 4: "Validity: yyyy.MM.dd-yyyy.MM.dd ..."
 *   Row 6: Section headers (POL / POD / Freight / Note)
 *   Row 7: Column headers — Country | Port | Code | Country | Port | Code | 20'DC | 40'DC | 40'HQ | TS Port
 *   Row 8+: data rows, freight values in USD (carrier convention).
 */
@Slf4j
@Component
public class NcpeFakParser implements RatesheetParser {

    private static final String SHEET_NAME = "FAK";
    private static final int COL_POL_COUNTRY = 0;
    private static final int COL_POL_PORT    = 1;
    private static final int COL_POL_CODE    = 2;
    private static final int COL_POD_COUNTRY = 3;
    private static final int COL_POD_PORT    = 4;
    private static final int COL_POD_CODE    = 5;
    private static final int COL_RATE_20DC   = 6;
    private static final int COL_RATE_40DC   = 7;
    private static final int COL_RATE_40HC   = 8;
    private static final int COL_TS_PORT     = 9;

    private static final int DATA_START_ROW_IDX = 7;       // 0-based: row 8 in spreadsheet
    private static final String DEFAULT_CURRENCY = "USD";  // NCPE FAK convention

    private static final Pattern VALIDITY = Pattern.compile(
            "(\\d{4}[./-]\\d{2}[./-]\\d{2})\\s*[-–]\\s*(\\d{4}[./-]\\d{2}[./-]\\d{2})");
    private static final Pattern UPDATE_DATE = Pattern.compile(
            "(?i)update\\s*[:.]?\\s*(\\d{2}[./-]\\d{2}[./-]\\d{4})");

    @Override
    public String name() {
        return "NcpeFakParser";
    }

    @Override
    public boolean supports(String fileName, Workbook workbook) {
        if (fileName != null && fileName.toUpperCase(Locale.ROOT).contains("NCPE")) {
            return true;
        }
        Sheet sheet = workbook.getSheet(SHEET_NAME);
        if (sheet == null) return false;
        Row r1 = sheet.getRow(0);
        String a1 = CellReader.string(r1, 0);
        return a1 != null && a1.toUpperCase(Locale.ROOT).contains("NCP");
    }

    @Override
    public CanonicalRatesheetDto parse(String fileName, Workbook workbook) {
        Sheet sheet = workbook.getSheet(SHEET_NAME);
        if (sheet == null) {
            throw new IllegalArgumentException("NCPE workbook missing 'FAK' sheet: " + fileName);
        }

        // Header metadata
        String validityCell = CellReader.string(sheet.getRow(3), 0); // row 4
        LocalDate[] validity = parseValidity(validityCell);

        String updateCell = CellReader.string(sheet.getRow(0), 3);   // D1
        String version = (updateCell != null) ? updateCell.trim() : null;

        // Data rows
        List<CanonicalRateLineDto> lines = new ArrayList<>();
        int last = sheet.getLastRowNum();
        for (int r = DATA_START_ROW_IDX; r <= last; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String polCode = CellReader.string(row, COL_POL_CODE);
            String podCode = CellReader.string(row, COL_POD_CODE);
            if (polCode == null || podCode == null) continue;
            polCode = polCode.trim().toUpperCase(Locale.ROOT);
            podCode = podCode.trim().toUpperCase(Locale.ROOT);
            if (polCode.length() != 5 || podCode.length() != 5) continue;

            String polPort = CellReader.string(row, COL_POL_PORT);
            String podPort = CellReader.string(row, COL_POD_PORT);
            String tsPort  = CellReader.string(row, COL_TS_PORT);

            BigDecimal r20 = CellReader.number(row, COL_RATE_20DC);
            BigDecimal r40 = CellReader.number(row, COL_RATE_40DC);
            BigDecimal r40h = CellReader.number(row, COL_RATE_40HC);

            addIfPresent(lines, polCode, polPort, podCode, podPort, tsPort, Equipment.DC20, r20);
            addIfPresent(lines, polCode, polPort, podCode, podPort, tsPort, Equipment.DC40, r40);
            addIfPresent(lines, polCode, polPort, podCode, podPort, tsPort, Equipment.HC40, r40h);
        }

        log.info("NCPE parsed {} rate lines from {}", lines.size(), fileName);

        return CanonicalRatesheetDto.builder()
                .carrierScac("NCPE")
                .carrierName("NCP Express")
                .source("NCPE_DRY_FAK_DE_NL_BE")
                .sourceFile(fileName)
                .contractNo(null)
                .type(RatesheetType.FAK)
                .currency(DEFAULT_CURRENCY)
                .validFrom(validity[0])
                .validTo(validity[1])
                .version(version)
                .lines(lines)
                .build();
    }

    private static void addIfPresent(List<CanonicalRateLineDto> sink,
                                     String pol, String polName,
                                     String pod, String podName,
                                     String via,
                                     Equipment eq, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) return;
        sink.add(CanonicalRateLineDto.builder()
                .pol(pol).polName(polName)
                .pod(pod).podName(podName)
                .via(via)
                .equipment(eq)
                .commodity("FAK")
                .baseAmount(amount)
                .currency(DEFAULT_CURRENCY)
                .build());
    }

    /** Extract "yyyy.MM.dd-yyyy.MM.dd" → LocalDate range. */
    static LocalDate[] parseValidity(String text) {
        if (text != null) {
            Matcher m = VALIDITY.matcher(text);
            if (m.find()) {
                LocalDate from = parseDate(m.group(1));
                LocalDate to   = parseDate(m.group(2));
                if (from != null && to != null) return new LocalDate[]{from, to};
            }
        }
        // Sensible fallback: validity unknown → today..+30d
        LocalDate today = LocalDate.now();
        return new LocalDate[]{today, today.plusDays(30)};
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null) return null;
        String[] p = raw.split("[./-]");
        if (p.length != 3) return null;
        try {
            // Detect order: yyyy first or last
            if (p[0].length() == 4) {
                return LocalDate.of(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
            }
            return LocalDate.of(Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
        } catch (Exception e) {
            return null;
        }
    }
}

