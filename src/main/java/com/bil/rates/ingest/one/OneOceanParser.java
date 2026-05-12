package com.bil.rates.ingest.one;

import com.bil.rates.domain.Equipment;
import com.bil.rates.domain.RatesheetType;
import com.bil.rates.ingest.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ONE Ocean Network Express shared parser.
 * Covers: AET EB (Asia Export), IET SB (Intra Europe Southbound), ZFS (South Africa).
 *
 * All three files use the same DRY sheet format:
 * Row 1 = headers: EffectiveDate | ExpiryDate | ServiceScope | CommodityGroupName | POR | POR Description | OriginTerm | DEL | DEL Description | DestinationTerm | 20DC | 40DC | 40HC ...
 * Data starts row 2.
 */
@Slf4j
@Component
public class OneOceanParser implements RatesheetParser {

    private static final String CARRIER_SCAC = "ONE";
    private static final String CARRIER_NAME = "Ocean Network Express (ONE)";

    @Override
    public String name() { return "OneOceanParser"; }

    @Override
    public boolean supports(String fileName, Workbook wb) {
        if (fileName == null) return false;
        String fn = fileName.toUpperCase();
        // Matches AET EB, IET SB, ZFS ONE ratesheets
        return fn.contains("ONE") && (fn.contains("AET") || fn.contains("IET") || fn.contains("ZFS"));
    }

    @Override
    public CanonicalRatesheetDto parse(String fileName, Workbook wb) {
        List<CanonicalRateLineDto> lines = new ArrayList<>();
        LocalDate[] validity = {null, null};

        // Each file has 1-3 DRY data sheets
        for (int si = 0; si < wb.getNumberOfSheets(); si++) {
            Sheet sheet = wb.getSheetAt(si);
            String sn = sheet.getSheetName().trim();
            if (!sn.toUpperCase().contains("DRY")) continue;
            parseSheet(sheet, lines, validity);
        }

        String source = deriveSource(fileName);
        RatesheetType type = RatesheetType.FAK;

        return CanonicalRatesheetDto.builder()
                .carrierScac(CARRIER_SCAC)
                .carrierName(CARRIER_NAME)
                .source(source)
                .sourceFile(fileName)
                .type(type)
                .currency("USD")
                .validFrom(validity[0] != null ? validity[0] : LocalDate.of(2026, 4, 1))
                .validTo(validity[1] != null ? validity[1] : LocalDate.of(2026, 4, 30))
                .lines(lines)
                .build();
    }

    private void parseSheet(Sheet sheet, List<CanonicalRateLineDto> out, LocalDate[] validity) {
        // Row 1 (index 0): header row
        Row header = sheet.getRow(0);
        if (header == null) return;

        // Detect column positions from header row
        int colEffDate = -1, colExpDate = -1, colPor = -1, colPorDesc = -1,
            colDel = -1, colDelDesc = -1;
        int col20dc = -1, col40dc = -1, col40hc = -1;

        for (int c = 0; c <= header.getLastCellNum(); c++) {
            String h = CellReader.string(header, c);
            if (h == null) continue;
            String hu = h.trim().toUpperCase();
            if (colEffDate < 0 && hu.contains("EFFECTIVE")) colEffDate = c;
            else if (colExpDate < 0 && hu.contains("EXPIRY")) colExpDate = c;
            else if (colPor < 0 && (hu.equals("POR") || hu.equals("POL"))) colPor = c;
            else if (colPorDesc < 0 && hu.contains("POR DESC")) colPorDesc = c;
            else if (colDel < 0 && (hu.equals("DEL") || hu.equals("POD"))) colDel = c;
            else if (colDelDesc < 0 && hu.contains("DEL DESC")) colDelDesc = c;
            else if (col20dc < 0 && hu.contains("20")) col20dc = c;
            else if (col40dc < 0 && hu.contains("40") && !hu.contains("HC") && !hu.contains("HQ")) col40dc = c;
            else if (col40hc < 0 && (hu.contains("40") && (hu.contains("HC") || hu.contains("HQ")))) col40hc = c;
        }

        // Fallback column positions (observed in real files)
        if (colPor < 0)   colPor = 4;
        if (colDel < 0)   colDel = 7;
        if (col20dc < 0)  col20dc = 10;
        if (col40dc < 0)  col40dc = 11;
        if (col40hc < 0)  col40hc = 12;

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String pol = CellReader.string(row, colPor);
            String pod = CellReader.string(row, colDel);
            if (pol == null || pod == null) continue;
            if (pol.length() != 5 || pod.length() != 5) continue;

            String polDesc = colPorDesc >= 0 ? CellReader.string(row, colPorDesc) : null;
            String podDesc = colDelDesc >= 0 ? CellReader.string(row, colDelDesc) : null;

            // Parse validity from first data row
            if (validity[0] == null && colEffDate >= 0) {
                validity[0] = readDate(row, colEffDate);
                validity[1] = readDate(row, colExpDate);
            }

            var dc20 = CellReader.number(row, col20dc);
            var dc40 = CellReader.number(row, col40dc);
            var hc40 = CellReader.number(row, col40hc);

            if (dc20 != null) out.add(build(pol, polDesc, pod, podDesc, Equipment.DC20, dc20, "USD"));
            if (dc40 != null) out.add(build(pol, polDesc, pod, podDesc, Equipment.DC40, dc40, "USD"));
            if (hc40 != null) out.add(build(pol, polDesc, pod, podDesc, Equipment.HC40, hc40, "USD"));
        }
    }

    private LocalDate readDate(Row row, int col) {
        if (col < 0) return null;
        Cell c = row.getCell(col);
        if (c == null) return null;
        try {
            if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
                return c.getLocalDateTimeCellValue().toLocalDate();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private CanonicalRateLineDto build(String pol, String polName, String pod, String podName,
                                        Equipment eq, java.math.BigDecimal amount, String cur) {
        return CanonicalRateLineDto.builder()
                .pol(pol).polName(polName)
                .pod(pod).podName(podName)
                .equipment(eq).baseAmount(amount).currency(cur)
                .commodity("FAK").build();
    }

    private String deriveSource(String fileName) {
        String fn = fileName.toUpperCase();
        if (fn.contains("AET")) return "ONE_AET_EB";
        if (fn.contains("IET")) return "ONE_IET_SB";
        if (fn.contains("ZFS")) return "ONE_ZFS_SA";
        return "ONE_EXPORT";
    }
}

