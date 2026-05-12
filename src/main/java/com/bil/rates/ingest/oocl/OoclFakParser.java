package com.bil.rates.ingest.oocl;

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
 * OOCL Export FAK ratesheet parser.
 *
 * File: "REVISED OOCL Export FAK Fareast_Mideast_India_Australia_Intra Europe_ Africa..."
 * Sheets: "Far East+Japan", "Koper+Trieste", "Intra Europe"
 *
 * "Far East+Japan" header row index 9 (row 10):
 *   col1=POL, col2=POD, col3=20' Box, col4=40' Box, col5=40' HQ, col7=Loop
 */
@Slf4j
@Component
public class OoclFakParser implements RatesheetParser {

    private static final String CARRIER_SCAC = "OOLU";
    private static final String CARRIER_NAME = "Orient Overseas Container Line (OOCL)";

    @Override
    public String name() { return "OoclFakParser"; }

    @Override
    public boolean supports(String fileName, Workbook wb) {
        if (fileName == null) return false;
        String fn = fileName.toUpperCase();
        return fn.contains("OOCL");
    }

    @Override
    public CanonicalRatesheetDto parse(String fileName, Workbook wb) {
        List<CanonicalRateLineDto> lines = new ArrayList<>();

        for (int si = 0; si < wb.getNumberOfSheets(); si++) {
            Sheet sheet = wb.getSheetAt(si);
            String sn = sheet.getSheetName().toUpperCase();
            if (sn.contains("INTRODUCTION") || sn.contains("LOOKUP") || sn.contains("RECHENBLA")) continue;
            parseSheet(sheet, lines);
        }

        return CanonicalRatesheetDto.builder()
                .carrierScac(CARRIER_SCAC)
                .carrierName(CARRIER_NAME)
                .source("OOCL_FAK_EXPORT")
                .sourceFile(fileName)
                .type(RatesheetType.FAK)
                .currency("USD")
                .validFrom(LocalDate.of(2025, 12, 1))
                .validTo(LocalDate.of(2026, 1, 31))
                .lines(lines)
                .build();
    }

    private void parseSheet(Sheet sheet, List<CanonicalRateLineDto> out) {
        // Find header row with "POL" in col 1
        int headerRow = -1;
        for (int i = 5; i <= 15; i++) {
            Row r = sheet.getRow(i);
            if (r == null) continue;
            String v = CellReader.string(r, 1);
            if ("POL".equalsIgnoreCase(v)) { headerRow = i; break; }
        }
        if (headerRow < 0) return;

        // Detect col positions from header
        Row hdr = sheet.getRow(headerRow);
        int colPol = 1, colPod = 2, col20 = 3, col40 = 4, col40hq = 5;
        for (int c = 1; c <= 10; c++) {
            String h = CellReader.string(hdr, c);
            if (h == null) continue;
            String hu = h.toUpperCase().replaceAll("[^A-Z0-9]", "");
            if (hu.equals("POL")) colPol = c;
            else if (hu.equals("POD")) colPod = c;
            else if (hu.contains("20")) col20 = c;
            else if (hu.contains("40") && !hu.contains("HQ") && !hu.contains("HC")) col40 = c;
            else if (hu.contains("40") && (hu.contains("HQ") || hu.contains("HC"))) col40hq = c;
        }

        String currentPol = null;

        for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String polCell = CellReader.string(row, colPol);
            if (polCell != null && !polCell.isBlank()) {
                currentPol = polCell.trim();
            }

            String pod = CellReader.string(row, colPod);
            if (pod == null || pod.isBlank()) continue;
            if (pod.toUpperCase().contains("POD")) continue;

            BigDecimal dc20 = CellReader.number(row, col20);
            BigDecimal dc40 = CellReader.number(row, col40);
            BigDecimal hc40 = CellReader.number(row, col40hq);

            if (currentPol == null) continue;

            // Expand multi-POL: "HAM / RTM / ANR"
            for (String polToken : currentPol.split("[/;]")) {
                String pol = mapLocode(polToken.trim());
                if (pol == null) continue;
                String podCode = mapLocode(pod.trim());
                String cleaned = pod.replaceAll("\\s.*", "").toUpperCase();
                String finalPod = podCode != null ? podCode
                        : (cleaned.length() >= 2 ? cleaned.substring(0, Math.min(5, cleaned.length())) : null);
                if (finalPod == null) continue;

                if (dc20 != null) out.add(line(pol, finalPod, pod, Equipment.DC20, dc20, "USD"));
                if (dc40 != null) out.add(line(pol, finalPod, pod, Equipment.DC40, dc40, "USD"));
                if (hc40 != null) out.add(line(pol, finalPod, pod, Equipment.HC40, hc40, "USD"));
            }
        }
    }

    private CanonicalRateLineDto line(String pol, String pod, String podName,
                                       Equipment eq, BigDecimal amount, String cur) {
        return CanonicalRateLineDto.builder()
                .pol(pol).pod(pod).podName(podName)
                .equipment(eq).baseAmount(amount).currency(cur)
                .commodity("FAK").build();
    }

    private String mapLocode(String s) {
        if (s == null) return null;
        String u = s.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return switch (u) {
            case "HAM", "HAMBURG"             -> "DEHAM";
            case "RTM", "ROTTERDAM"           -> "NLRTM";
            case "ANR", "ANTWERP"             -> "BEANR";
            case "BHV", "BRV", "BREMERHAVEN" -> "DEBRV";
            case "WVN", "WILHELMSHAVEN"       -> "DEWVN";
            case "ZEE", "ZEEBRUGGE"           -> "BEZEE";
            case "KOPER"                      -> "SIKOP";
            case "TRIESTE"                    -> "ITTRS";
            case "DALIAN"                     -> "CNDLC";
            case "HONGKONG"                   -> "HKHKG";
            case "KAOHSIUNG"                  -> "TWKHH";
            case "NANSHA"                     -> "CNNSA";
            case "NINGBO"                     -> "CNNGB";
            case "PORTKLANG"                  -> "MYPKG";
            case "QINGDAO"                    -> "CNTAO";
            case "SHANGHAI"                   -> "CNSHA";
            case "SHEKOU"                     -> "CNSHK";
            case "ISTANBUL", "KUMPORT"        -> "TRIST";
            case "KORFEZ", "YARIMCA"          -> "TRKRF";
            case "GEMLIK"                     -> "TRGEM";
            case "ALIAGAIZMIR", "ALIAGA"      -> "TRALI";
            case "PIRAEUS"                    -> "GRPIR";
            case "MERSIN"                     -> "TRMER";
            case "ISKENDERUN"                 -> "TRISK";
            case "SINGAPORE"                  -> "SGSIN";
            case "BUSAN"                      -> "KRBSN";
            default -> s.length() == 5 ? s.toUpperCase() : null;
        };
    }
}

