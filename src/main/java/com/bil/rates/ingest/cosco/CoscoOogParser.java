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
 * COSCO FAK OOG ratesheet parser.
 *
 * File: "COSCO SHIPPING LINES GERMANY - FAK RATESHEET OOG - 01.04. - 30.04.2026 - V2.xlsx"
 * Sheets: " FE - OOG ", " ME-IPBC - OOG", "SA East Coast-OOG", "SA Westcoast - OOG", "OCEANIA - OOG"
 *
 * Header starts at row index 7 (row 8):
 *   col1=POL, col2=AREA, col3=POD, col4=20'OT FR in gauge, col5=40'OT/OQ/FR/FQ in gauge,
 *   col6=20' OH, col7=40' OH, col8=20' OW, col9=40' OW, col10=20' OW+OH, col11=40' OW+OH
 */
@Slf4j
@Component
public class CoscoOogParser implements RatesheetParser {

    private static final String CARRIER_SCAC = "COSU";
    private static final String CARRIER_NAME = "COSCO Shipping Lines";

    @Override
    public String name() { return "CoscoOogParser"; }

    @Override
    public boolean supports(String fileName, Workbook wb) {
        if (fileName == null) return false;
        String fn = fileName.toUpperCase();
        return fn.contains("COSCO") && fn.contains("OOG") && !fn.contains("IET");
    }

    @Override
    public CanonicalRatesheetDto parse(String fileName, Workbook wb) {
        List<CanonicalRateLineDto> lines = new ArrayList<>();

        for (int si = 0; si < wb.getNumberOfSheets(); si++) {
            Sheet sheet = wb.getSheetAt(si);
            parseSheet(sheet, lines);
        }

        return CanonicalRatesheetDto.builder()
                .carrierScac(CARRIER_SCAC)
                .carrierName(CARRIER_NAME)
                .source("COSCO_FAK_OOG")
                .sourceFile(fileName)
                .type(RatesheetType.OOG)
                .currency("USD")
                .validFrom(LocalDate.of(2026, 4, 1))
                .validTo(LocalDate.of(2026, 4, 30))
                .lines(lines)
                .build();
    }

    private void parseSheet(Sheet sheet, List<CanonicalRateLineDto> out) {
        String sheetName = sheet.getSheetName().toUpperCase();
        // Only OOG content sheets
        if (!sheetName.contains("OOG") && !sheetName.contains("FE") && !sheetName.contains("ME")
                && !sheetName.contains("SA") && !sheetName.contains("OCEANIA")) return;

        // Find currency from row 10 (index 9)
        String currency = "USD";
        Row currRow = sheet.getRow(9);
        if (currRow != null) {
            String cur = CellReader.string(currRow, 2);
            if ("EUR".equalsIgnoreCase(cur)) currency = "EUR";
        }

        // Header row at index 7 (row 8)
        final int DATA_START = 16; // Data typically starts around row 17-18

        String currentPol = null;

        for (int i = DATA_START; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String polCell = CellReader.string(row, 1);
            if (polCell != null && !polCell.isBlank() && polCell.length() < 60) {
                // Multi-port cells like "HAMBURG\nROTTERDAM\nANTWERP"
                currentPol = polCell.trim();
            }

            String pod = CellReader.string(row, 3);
            if (pod == null || pod.isBlank() || pod.length() > 50) continue;
            if (pod.toUpperCase().contains("POD") || pod.toUpperCase().contains("FILING")) continue;

            // Columns 4,5 = in-gauge (OT20, OT40); 8,9 = OW; 10,11 = OW+OH
            BigDecimal ot20 = CellReader.number(row, 4);
            BigDecimal ot40 = CellReader.number(row, 5);
            BigDecimal ow20 = CellReader.number(row, 8);
            BigDecimal ow40 = CellReader.number(row, 9);

            if (currentPol == null) continue;

            for (String polRaw : currentPol.split("[\n/]")) {
                String pol = nameToLocode(polRaw.trim());
                if (pol == null) continue;
                String podCode = nameToLocode(pod.trim());
                String finalPod = podCode != null ? podCode : pod.toUpperCase().replaceAll("\\s.*", "").substring(0, Math.min(5, pod.length()));

                if (ot20 != null) out.add(line(pol, finalPod, pod, Equipment.OT20, ot20, currency));
                if (ot40 != null) out.add(line(pol, finalPod, pod, Equipment.OT40, ot40, currency));
                if (ow20 != null) out.add(line(pol, finalPod, pod, Equipment.FR20, ow20, currency));
                if (ow40 != null) out.add(line(pol, finalPod, pod, Equipment.FR40, ow40, currency));
            }
        }
    }

    private CanonicalRateLineDto line(String pol, String pod, String podName,
                                       Equipment eq, BigDecimal amount, String cur) {
        return CanonicalRateLineDto.builder()
                .pol(pol).pod(pod).podName(podName)
                .equipment(eq).baseAmount(amount).currency(cur)
                .commodity("OOG").build();
    }

    private String nameToLocode(String s) {
        if (s == null) return null;
        // Clean HTML-like linebreaks and extra whitespace
        s = s.replaceAll("(?i)<br>|\\n|\\r", " ").trim();
        String u = s.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return switch (u) {
            case "HAMBURG"           -> "DEHAM";
            case "ROTTERDAM"         -> "NLRTM";
            case "ANTWERP"           -> "BEANR";
            case "BREMERHAVEN"       -> "DEBRV";
            case "WILHELMSHAVEN"     -> "DEWVN";
            case "NANSHA"            -> "CNNSA";
            case "DALIAN"            -> "CNDLC";
            case "SHANGHAI"          -> "CNSHA";
            case "QINGDAO"           -> "CNTAO";
            case "NINGBO"            -> "CNNGB";
            case "XINGANG", "TIANJIN"-> "CNTXG";
            case "SHEKOU"            -> "CNSHK";
            case "YANTIAN"           -> "CNYTN";
            case "SINGAPORE"         -> "SGSIN";
            case "NHAVASHEVA"        -> "INNSA";
            case "MUNDRA"            -> "INMUN";
            case "KARACHI"           -> "PKKAR";
            case "COLOMBO"           -> "LKCMB";
            case "SANTOS"            -> "BRSSZ";
            case "PARANAGUA"         -> "BRPNZ";
            case "CALLAO"            -> "PECLL";
            case "CARTAGENA"         -> "COBAQ";
            case "CAUCEDO"           -> "DOSDC";
            case "ADELAIDE"          -> "AUADL";
            case "BRISBANE"          -> "AUBNE";
            case "SYDNEY"            -> "AUSYD";
            case "MELBOURNE"         -> "AUMEL";
            case "FREMANTLE"         -> "AUFRE";
            default -> s.length() == 5 ? s.toUpperCase() : null;
        };
    }
}

