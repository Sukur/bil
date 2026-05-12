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
 * COSCO FAK Reefer parser.
 *
 * File: "COSCO SHIPPING LINES GERMANY - FAK RATESHEET REEFER - 01.04. - 30.04.2026 - V1.xlsx"
 * Sheets: "Reefer FE", "Reefer ME & IPBC", "FE & ME+IPBC Outports",
 *         "Reefer SA East Coast", "Reefer SA Westcoast"
 *
 * Header row index 7 (row 8):
 *   col1=POL, col2=AREA, col3=POD, col4=CARRIER EQUIPMENT (label row),
 *   row index 14 (row 15): col4=20RF, col5=40RQ
 */
@Slf4j
@Component
public class CoscoReeferParser implements RatesheetParser {

    private static final String CARRIER_SCAC = "COSU";
    private static final String CARRIER_NAME = "COSCO Shipping Lines";

    @Override
    public String name() { return "CoscoReeferParser"; }

    @Override
    public boolean supports(String fileName, Workbook wb) {
        if (fileName == null) return false;
        String fn = fileName.toUpperCase();
        return fn.contains("COSCO") && fn.contains("REEFER") && !fn.contains("IET");
    }

    @Override
    public CanonicalRatesheetDto parse(String fileName, Workbook wb) {
        List<CanonicalRateLineDto> lines = new ArrayList<>();

        for (int si = 0; si < wb.getNumberOfSheets(); si++) {
            Sheet sheet = wb.getSheetAt(si);
            String sn = sheet.getSheetName().toUpperCase();
            if (sn.contains("REEFER") || sn.contains("FE") || sn.contains("ME")) {
                parseSheet(sheet, lines);
            }
        }

        return CanonicalRatesheetDto.builder()
                .carrierScac(CARRIER_SCAC)
                .carrierName(CARRIER_NAME)
                .source("COSCO_FAK_REEFER")
                .sourceFile(fileName)
                .type(RatesheetType.REEFER)
                .currency("USD")
                .validFrom(LocalDate.of(2026, 4, 1))
                .validTo(LocalDate.of(2026, 4, 30))
                .lines(lines)
                .build();
    }

    private void parseSheet(Sheet sheet, List<CanonicalRateLineDto> out) {
        // Determine currency from row 10 (index 9)
        String currency = "USD";
        Row currRow = sheet.getRow(9);
        if (currRow != null) {
            String cur = CellReader.string(currRow, 2);
            if ("EUR".equalsIgnoreCase(cur)) currency = "EUR";
        }

        // Data rows typically start at row index 15
        final int DATA_START = 15;
        String currentPol = null;

        for (int i = DATA_START; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String polCell = CellReader.string(row, 1);
            if (polCell != null && !polCell.isBlank() && polCell.length() < 50) {
                currentPol = polCell.trim();
            }

            String pod = CellReader.string(row, 3);
            if (pod == null || pod.isBlank() || pod.length() > 50) continue;
            if (pod.toUpperCase().contains("POD") || pod.toUpperCase().contains("DESTINATION")) continue;

            BigDecimal rf20 = CellReader.number(row, 4);
            BigDecimal rq40 = CellReader.number(row, 5);
            BigDecimal hcrf = CellReader.number(row, 6); // some sheets have 40HCRF NOR

            if (currentPol == null) continue;

            for (String polRaw : currentPol.split("[\n/]")) {
                String pol = nameToLocode(polRaw.trim());
                if (pol == null) continue;
                String podCode = nameToLocode(pod.trim());
                String cleaned = pod.toUpperCase().replaceAll("[^A-Z0-9].*", "");
                String finalPod = podCode != null ? podCode
                        : (cleaned.length() >= 2 ? cleaned.substring(0, Math.min(5, cleaned.length())) : null);
                if (finalPod == null) continue;

                if (rf20 != null) out.add(line(pol, finalPod, pod, Equipment.RF20, rf20, currency));
                if (rq40 != null) out.add(line(pol, finalPod, pod, Equipment.HR40, rq40, currency));
                if (hcrf != null && !hcrf.equals(rq40)) out.add(line(pol, finalPod, pod, Equipment.RF40, hcrf, currency));
            }
        }
    }

    private CanonicalRateLineDto line(String pol, String pod, String podName,
                                       Equipment eq, BigDecimal amount, String cur) {
        return CanonicalRateLineDto.builder()
                .pol(pol).pod(pod).podName(podName)
                .equipment(eq).baseAmount(amount).currency(cur)
                .commodity("REEFER").build();
    }

    private String nameToLocode(String s) {
        if (s == null) return null;
        String u = s.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return switch (u) {
            case "HAMBURG"           -> "DEHAM";
            case "ROTTERDAM"         -> "NLRTM";
            case "ANTWERP"           -> "BEANR";
            case "BREMERHAVEN"       -> "DEBRV";
            case "WILHELMSHAVEN"     -> "DEWVN";
            case "DALIAN"            -> "CNDLC";
            case "SHANGHAI"          -> "CNSHA";
            case "QINGDAO"           -> "CNTAO";
            case "NINGBO"            -> "CNNGB";
            case "TIANJIN", "XINGANG", "TIANJINXINGANG" -> "CNTXG";
            case "XIAMEN"            -> "CNXMN";
            case "YANTIAN"           -> "CNYTN";
            case "NANSHA"            -> "CNNSA";
            case "HONGKONG"          -> "HKHKG";
            case "BUSAN", "PUSAN"    -> "KRBSN";
            case "KAOHSIUNG"         -> "TWKHH";
            case "SINGAPORE"         -> "SGSIN";
            case "PORTKELANG", "PORTKLANG" -> "MYPKG";
            case "NHAVASHEVA"        -> "INNSA";
            case "MUNDRA"            -> "INMUN";
            case "COLOMBO"           -> "LKCMB";
            case "KARACHI"           -> "PKKAR";
            case "PARANAGUA"         -> "BRPNZ";
            case "SANTOS"            -> "BRSSZ";
            case "RIODEJANEIRO"      -> "BRRJO";
            case "ITAJAI"            -> "BRITJ";
            case "CAUCEDO"           -> "DOSDC";
            case "CARTAGENA"         -> "COBAQ";
            case "BUENAVENTURA"      -> "COBUN";
            case "MANZANILLO"        -> "PAMIT";
            default -> s.length() == 5 ? s.toUpperCase() : null;
        };
    }
}

