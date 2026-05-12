package com.bil.rates.ingest.hmm;

import com.bil.rates.domain.Equipment;
import com.bil.rates.ingest.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * HMM Standard ratesheet parser.
 *
 * File: "CJ ICM Logistics GmbH - HMM Ratesheet Germany April 2026.xlsx"
 * Data sheets: "3. Main Ports I FIM I TW", "4. Japan Outports", "5. China I SE Asia"
 * Header row: 30 (0-based 29)
 * Columns: POL col3, LOOP col12, POD col21+, then equipment columns 29..47 (20DC/40DC/40HC pattern)
 *
 * The actual rate columns depend on the section header. We dynamically detect DC20/DC40/HC40 columns.
 */
@Slf4j
@Component
public class HmmStandardParser implements RatesheetParser {

    private static final String CARRIER_SCAC = "HMM";
    private static final String CARRIER_NAME = "HMM Co. Ltd.";

    @Override
    public String name() { return "HmmStandardParser"; }

    @Override
    public boolean supports(String fileName, Workbook wb) {
        if (fileName == null) return false;
        String fn = fileName.toUpperCase();
        // Matches HMM standard ratesheet (not the Special/OOG one)
        return fn.contains("HMM") && fn.contains("RATESHEET") && !fn.contains("SPECIAL");
    }

    @Override
    public CanonicalRatesheetDto parse(String fileName, Workbook wb) {
        List<CanonicalRateLineDto> lines = new ArrayList<>();

        String[] sheetNames = {
            "3. Main Ports I FIM I TW",
            "4. Japan Outports",
            "5. China I SE Asia"
        };

        for (String sheetName : sheetNames) {
            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null) continue;
            parseSheet(sheet, lines);
        }

        return CanonicalRatesheetDto.builder()
                .carrierScac(CARRIER_SCAC)
                .carrierName(CARRIER_NAME)
                .source("HMM_GER_STANDARD")
                .sourceFile(fileName)
                .type(com.bil.rates.domain.RatesheetType.FAK)
                .currency("USD")
                .validFrom(LocalDate.of(2026, 4, 1))
                .validTo(LocalDate.of(2026, 4, 30))
                .lines(lines)
                .build();
    }

    private void parseSheet(Sheet sheet, List<CanonicalRateLineDto> out) {
        // Find the header row by looking for "POL" in col 2 (0-indexed)
        int headerRow = -1;
        for (int i = sheet.getFirstRowNum(); i <= Math.min(sheet.getLastRowNum(), 40); i++) {
            Row r = sheet.getRow(i);
            if (r == null) continue;
            String c2 = CellReader.string(r, 2);
            String c20 = CellReader.string(r, 20);
            if ("POL".equalsIgnoreCase(c2) && c20 != null && c20.toUpperCase().contains("POD")) {
                headerRow = i;
                break;
            }
        }
        if (headerRow < 0) return;

        // Equipment columns are in the SAME header row (single-row header)
        // Actual labels: "20' DRY CNTR", "40' DRY CNTR", "40' HIGH CUBE"
        Row hdrRow = sheet.getRow(headerRow);
        int col20dc = -1, col40dc = -1, col40hc = -1;
        if (hdrRow != null) {
            for (int c = 20; c <= 60 && c < hdrRow.getLastCellNum(); c++) {
                String val = CellReader.string(hdrRow, c);
                if (val == null) continue;
                String v = val.toUpperCase().replaceAll("[^A-Z0-9]", "");
                if (col20dc < 0 && v.contains("20") && (v.contains("DRY") || v.contains("DC") || v.contains("GP"))) col20dc = c;
                else if (col40dc < 0 && v.contains("40") && (v.contains("DRY") || v.contains("DC") || v.contains("GP"))
                        && !v.contains("HIGH") && !v.contains("HC") && !v.contains("HQ")) col40dc = c;
                else if (col40hc < 0 && v.contains("40") && (v.contains("HIGH") || v.contains("HC") || v.contains("HQ"))) col40hc = c;
            }
        }
        // Fallback to known column positions for this HMM standard ratesheet layout
        if (col20dc < 0) col20dc = 32;
        if (col40dc < 0) col40dc = 39;
        if (col40hc < 0) col40hc = 46;

        log.debug("HmmStandard sheet={} headerRow={} col20dc={} col40dc={} col40hc={}",
                sheet.getSheetName(), headerRow, col20dc, col40dc, col40hc);

        String currentPol = null;

        for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String polCell = CellReader.string(row, 2);
            if (polCell != null && !polCell.isBlank()) {
                currentPol = polCell.trim();
            }
            if (currentPol == null) continue;

            String pod = CellReader.string(row, 20);
            if (pod == null || pod.isBlank()) continue;
            if (pod.equalsIgnoreCase("POD") || pod.length() > 50) continue;

            BigDecimal dc20 = CellReader.number(row, col20dc);
            BigDecimal dc40 = CellReader.number(row, col40dc);
            BigDecimal hc40 = CellReader.number(row, col40hc);
            // Skip rows with no rates at all
            if (dc20 == null && dc40 == null && hc40 == null) continue;

            // Expand multi-POL like "HAM / RTM / ANR"
            String[] pols = currentPol.split("[/\n]");
            for (String polRaw : pols) {
                String pol = extractLocode(polRaw.trim());
                if (pol == null) continue;
                String podCode = extractLocode(pod.trim());
                String cleaned = pod.toUpperCase().replaceAll("\\s.*", "");
                String finalPod = podCode != null ? podCode
                        : (cleaned.length() >= 2 ? cleaned.substring(0, Math.min(5, cleaned.length())) : null);
                if (finalPod == null) continue;

                if (dc20 != null) out.add(line(pol, finalPod, Equipment.DC20, dc20, "USD"));
                if (dc40 != null) out.add(line(pol, finalPod, Equipment.DC40, dc40, "USD"));
                if (hc40 != null) out.add(line(pol, finalPod, Equipment.HC40, hc40, "USD"));
            }
        }
    }

    /** Try to extract a 5-char UN/LOCODE from a string like "HAM / RTM / ANR" → multiple. */
    private String extractLocode(String s) {
        if (s == null) return null;
        s = s.trim();
        return switch (s.toUpperCase().replaceAll("[^A-Z0-9]", "")) {
            case "HAM", "HAMBURG"             -> "DEHAM";
            case "RTM", "ROTTERDAM"           -> "NLRTM";
            case "ANR", "ANTWERP", "ANTWERPEN" -> "BEANR";
            case "BRV", "BHV", "BREMERHAVEN"  -> "DEBRV";
            case "WVN", "WILHELMSHAVEN"        -> "DEWVN";
            case "ZEE", "ZEEBRUGGE"           -> "BEZEE";
            case "BUSAN", "PUSAN"             -> "KRBSN";
            case "SINGAPORE"                  -> "SGSIN";
            case "SHANGHAI"                   -> "CNSHA";
            case "NINGBO"                     -> "CNNGB";
            case "QINGDAO"                    -> "CNTAO";
            case "TIANJIN", "XINGANG"         -> "CNTXG";
            case "DALIAN"                     -> "CNDLC";
            case "YANTIAN"                    -> "CNYTN";
            case "SHEKOU"                     -> "CNSHK";
            case "NANSHA"                     -> "CNNSA";
            case "XIAMEN"                     -> "CNXMN";
            case "HONGKONG"                   -> "HKHKG";
            case "KAOHSIUNG"                  -> "TWKHH";
            case "TOKYO", "YOKOHAMA"          -> "JPYOK";
            case "OSAKA"                      -> "JPOSA";
            case "NAGOYA"                     -> "JPNGO";
            case "KOBE"                       -> "JPKOB";
            case "NHAVASHEVA"                 -> "INNSA";
            case "MUNDRA"                     -> "INMUN";
            case "KARACHI"                    -> "PKKAR";
            case "PORTKELANG", "PORTKLANG"    -> "MYPKG";
            case "LAEMCHABANG"                -> "THLCH";
            case "HOCHIMINH"                  -> "VNSGN";
            case "JAKARTA"                    -> "IDJKT";
            case "MANILA"                     -> "PHMNL";
            default -> s.length() == 5 ? s.toUpperCase() : null;
        };
    }

    private CanonicalRateLineDto line(String pol, String pod, Equipment eq, BigDecimal amount, String cur) {
        return CanonicalRateLineDto.builder()
                .pol(pol).pod(pod).equipment(eq).baseAmount(amount).currency(cur).build();
    }
}

