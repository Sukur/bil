package com.bil.rates.ingest.hmm;

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
 * HMM Special Equipment (OOG/OT/FR) ratesheet parser.
 *
 * File: "CJ ICM Logistics GmbH - HMM GER Special Ratesheet APR 2026.xlsx"
 * Sheet: "OOG"
 * Header row 9 (0-indexed 8):
 *   col0=POD, col1=Country, col2=POL, col3=Service, col4=20' OT/ig, col5=40' OT/ig,
 *   col6=20' FR/ig, col7=40' FR/ig, col8=20' OT/oh, col9=40' OT/oh, col10=20' FR/oh ...
 */
@Slf4j
@Component
public class HmmSpecialOogParser implements RatesheetParser {

    private static final String CARRIER_SCAC = "HMM";
    private static final String CARRIER_NAME = "HMM Co. Ltd.";

    @Override
    public String name() { return "HmmSpecialOogParser"; }

    @Override
    public boolean supports(String fileName, Workbook wb) {
        if (fileName == null) return false;
        String fn = fileName.toUpperCase();
        return fn.contains("HMM") && fn.contains("SPECIAL");
    }

    @Override
    public CanonicalRatesheetDto parse(String fileName, Workbook wb) {
        List<CanonicalRateLineDto> lines = new ArrayList<>();
        Sheet sheet = wb.getSheet("OOG");
        if (sheet == null && wb.getNumberOfSheets() > 0) sheet = wb.getSheetAt(0);
        if (sheet != null) parseSheet(sheet, lines);

        return CanonicalRatesheetDto.builder()
                .carrierScac(CARRIER_SCAC)
                .carrierName(CARRIER_NAME)
                .source("HMM_GER_SPECIAL_OOG")
                .sourceFile(fileName)
                .type(RatesheetType.OOG)
                .currency("USD")
                .validFrom(LocalDate.of(2026, 4, 1))
                .validTo(LocalDate.of(2026, 4, 30))
                .lines(lines)
                .build();
    }

    private void parseSheet(Sheet sheet, List<CanonicalRateLineDto> out) {
        // Header is at row index 8 (row 9)
        // col0=POD name, col2=POL, col4=20'OT ig, col5=40'OT ig, col6=20'FR ig, col7=40'FR ig
        // col8=20'OT oh, col9=40'OT oh, col10=20'FR oh, col11=40'FR oh
        final int HEADER = 8;

        for (int i = HEADER + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String podName = CellReader.string(row, 0);
            String polRaw  = CellReader.string(row, 2);
            if (podName == null || polRaw == null) continue;
            if (podName.toUpperCase().contains("POD") || podName.length() > 40) continue;

            // POD: derive LOCODE from name
            String pod = nameToLocode(podName);
            if (pod == null) {
                log.debug("HmmSpecial: no LOCODE for POD '{}' – skipping row {}", podName, i);
                continue;
            }
            BigDecimal ot20ig = CellReader.number(row, 4);
            BigDecimal ot40ig = CellReader.number(row, 5);
            BigDecimal fr20ig = CellReader.number(row, 6);
            BigDecimal fr40ig = CellReader.number(row, 7);

            // Expand multi-POL "EX RTM/HAM" → NLRTM, DEHAM
            String[] polTokens = polRaw.replaceFirst("(?i)^EX\\s*", "").split("[/ ,]+");
            for (String polToken : polTokens) {
                String pol = nameToLocode(polToken.trim());
                if (pol == null) continue;

                addLine(out, pol, null, pod, podName, Equipment.OT20, ot20ig);
                addLine(out, pol, null, pod, podName, Equipment.OT40, ot40ig);
                addLine(out, pol, null, pod, podName, Equipment.FR20, fr20ig);
                addLine(out, pol, null, pod, podName, Equipment.FR40, fr40ig);
            }
        }
    }

    private void addLine(List<CanonicalRateLineDto> out,
                         String pol, String polName, String pod, String podName,
                         Equipment eq, BigDecimal amount) {
        if (amount == null) return;
        out.add(CanonicalRateLineDto.builder()
                .pol(pol).polName(polName)
                .pod(pod).podName(podName)
                .equipment(eq).baseAmount(amount).currency("USD")
                .commodity("OOG").build());
    }

    private String nameToLocode(String s) {
        if (s == null) return null;
        return switch (s.toUpperCase().replaceAll("[^A-Z]", "")) {
            case "HAM", "HAMBURG"        -> "DEHAM";
            case "RTM", "ROTTERDAM"      -> "NLRTM";
            case "ANR", "ANTWERP"        -> "BEANR";
            case "BRV", "BREMERHAVEN"    -> "DEBRV";
            case "BUSAN", "PUSAN"        -> "KRBSN";
            case "KOBE"                  -> "JPKOB";
            case "TOKYO", "YOKOHAMA"     -> "JPYOK";
            case "OSAKA"                 -> "JPOSA";
            case "NAGOYA"                -> "JPNGO";
            case "SINGAPORE"             -> "SGSIN";
            case "SHANGHAI"              -> "CNSHA";
            case "NINGBO"                -> "CNNGB";
            case "QINGDAO"               -> "CNTAO";
            case "XINGANG", "TIANJIN"    -> "CNTXG";
            case "SHENZHEN", "SHEKOU"    -> "CNSHK";
            case "YANTIAN"               -> "CNYTN";
            case "GUANGZHOU", "NANSHA"   -> "CNNSA";
            case "HONGKONG", "HKHKG"     -> "HKHKG";
            case "JAKARTA"               -> "IDJKT";
            case "LAEMCHABANG"           -> "THLCH";
            case "HOCHIMINH", "HCM"      -> "VNSGN";
            case "KAOHSIUNG"             -> "TWKHH";
            default -> s.length() == 5 ? s.toUpperCase() : null;
        };
    }
}

