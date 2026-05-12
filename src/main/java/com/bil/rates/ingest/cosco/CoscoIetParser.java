package com.bil.rates.ingest.cosco;

import com.bil.rates.domain.Equipment;
import com.bil.rates.domain.RatesheetType;
import com.bil.rates.ingest.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * COSCO IET Intra-Europe Southbound parser.
 *
 * File: "COSCO SHIPPING LINES - IET SB RATE SHEETS - 01.04.2026 - 30.04.2026.xlsx"
 * Sheets: "IET VIP Southbound Main ports", "IET VIP Southbound out ports", "IET REEFER Southbound"
 *
 * Header row index 11 (row 12). Columns:
 *   col1=POL, col2=Country, col3=POD, col4=20DC EUR, col5=40DC EUR, col6=40HC EUR
 *   col8=THC origin, col9=SSL, col10=FAF, col11=ETS, col12=EBS
 */
@Slf4j
@Component
public class CoscoIetParser implements RatesheetParser {

    private static final String CARRIER_SCAC = "COSU";
    private static final String CARRIER_NAME = "COSCO Shipping Lines";

    @Override
    public String name() { return "CoscoIetParser"; }

    @Override
    public boolean supports(String fileName, Workbook wb) {
        if (fileName == null) return false;
        String fn = fileName.toUpperCase();
        return fn.contains("COSCO") && fn.contains("IET");
    }

    @Override
    public CanonicalRatesheetDto parse(String fileName, Workbook wb) {
        List<CanonicalRateLineDto> lines = new ArrayList<>();

        for (int si = 0; si < wb.getNumberOfSheets(); si++) {
            Sheet sheet = wb.getSheetAt(si);
            String sn = sheet.getSheetName().toUpperCase();
            if (sn.contains("TT OVERVIEW") || sn.contains("REEFER")) continue;
            parseMainSheet(sheet, lines, sn.contains("REEFER"));
        }
        // Also parse reefer
        for (int si = 0; si < wb.getNumberOfSheets(); si++) {
            Sheet sheet = wb.getSheetAt(si);
            if (sheet.getSheetName().toUpperCase().contains("REEFER")) {
                parseReeferSheet(sheet, lines);
            }
        }

        return CanonicalRatesheetDto.builder()
                .carrierScac(CARRIER_SCAC)
                .carrierName(CARRIER_NAME)
                .source("COSCO_IET_SB")
                .sourceFile(fileName)
                .type(RatesheetType.FAK)
                .currency("EUR")
                .validFrom(LocalDate.of(2026, 4, 1))
                .validTo(LocalDate.of(2026, 4, 30))
                .lines(lines)
                .build();
    }

    private void parseMainSheet(Sheet sheet, List<CanonicalRateLineDto> out, boolean reefer) {
        // Find data rows: header at row index ~17-18 (actual data cols labeled 20DC/40DC/40HC)
        // We'll scan for the row that has "20DC" in one of the cells
        int dataStartRow = -1;
        for (int i = 10; i <= 25; i++) {
            Row r = sheet.getRow(i);
            if (r == null) continue;
            for (int c = 3; c <= 8; c++) {
                String v = CellReader.string(r, c);
                if (v != null && v.toUpperCase().contains("20DC")) {
                    dataStartRow = i + 1;
                    break;
                }
            }
            if (dataStartRow >= 0) break;
        }
        if (dataStartRow < 0) dataStartRow = 19; // fallback

        String currentPol = null;

        for (int i = dataStartRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String polCell = CellReader.string(row, 1);
            if (polCell != null && !polCell.isBlank() && polCell.length() < 30) {
                currentPol = polCell.trim().toUpperCase();
            }

            String pod = CellReader.string(row, 3);
            if (pod == null || pod.isBlank() || pod.length() > 40) continue;
            if (pod.toUpperCase().contains("POD") || pod.toUpperCase().contains("VALIDITY")) continue;

            BigDecimal dc20 = CellReader.number(row, 4);
            BigDecimal dc40 = CellReader.number(row, 5);
            BigDecimal hc40 = CellReader.number(row, 6);

            if (currentPol == null) continue;
            String pol = mapLocode(currentPol);
            String podCode = mapLocode(pod);

            if (dc20 != null) out.add(line(pol, pod, podCode, Equipment.DC20, dc20, "EUR"));
            if (dc40 != null) out.add(line(pol, pod, podCode, Equipment.DC40, dc40, "EUR"));
            if (hc40 != null) out.add(line(pol, pod, podCode, Equipment.HC40, hc40, "EUR"));
        }
    }

    private void parseReeferSheet(Sheet sheet, List<CanonicalRateLineDto> out) {
        int dataStart = 19;
        String currentPol = null;

        for (int i = dataStart; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String polCell = CellReader.string(row, 1);
            if (polCell != null && !polCell.isBlank() && polCell.length() < 30) {
                currentPol = polCell.trim().toUpperCase();
            }

            String pod = CellReader.string(row, 3);
            if (pod == null || pod.isBlank() || pod.length() > 40) continue;

            BigDecimal rf20 = CellReader.number(row, 4);
            BigDecimal hcrf = CellReader.number(row, 5);

            if (currentPol == null) continue;
            String pol = mapLocode(currentPol);
            String podCode = mapLocode(pod);

            if (rf20  != null) out.add(line(pol, pod, podCode, Equipment.RF20, rf20, "EUR"));
            if (hcrf  != null) out.add(line(pol, pod, podCode, Equipment.HR40, hcrf, "EUR"));
        }
    }

    private CanonicalRateLineDto line(String polCode, String podName, String podCode,
                                       Equipment eq, BigDecimal amount, String cur) {
        return CanonicalRateLineDto.builder()
                .pol(polCode).pod(podCode != null ? podCode : podName.substring(0, Math.min(5, podName.length())).toUpperCase())
                .podName(podName).equipment(eq).baseAmount(amount).currency(cur)
                .commodity("FAK").build();
    }

    private String mapLocode(String s) {
        if (s == null) return null;
        String u = s.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return switch (u) {
            case "HAMBURG"          -> "DEHAM";
            case "BREMERHAVEN"      -> "DEBRV";
            case "ROTTERDAM"        -> "NLRTM";
            case "ANTWERP"          -> "BEANR";
            case "FELIXSTOWE"       -> "GBFXT";
            case "PIRAEUS"          -> "GRPIR";
            case "KUMPORT", "IST"   -> "TRIST";
            case "GEMLIK"           -> "TRGEM";
            case "ALIAGA"           -> "TRALI";
            case "YARIMCA"          -> "TRYAR";
            case "ALEXANDRETTA", "ISKENDERUN" -> "TRISK";
            case "MERSIN"           -> "TRMER";
            case "THESSALONIKI"     -> "GRSKG";
            case "HAIFA"            -> "ILHFA";
            case "ASHDOD"           -> "ILASH";
            case "BEIRUT"           -> "LBBEY";
            case "LIMASSOL"         -> "CYLMS";
            case "ALEXANDRIA"       -> "EGALY";
            case "CASABLANCA"       -> "MACAS";
            default -> s.length() == 5 ? s.toUpperCase() : null;
        };
    }
}

