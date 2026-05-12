package com.bil.rates.ingest;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Small POI helpers shared by parsers. */
public final class CellReader {
    private CellReader() {}

    public static String string(Row row, int colIndex0) {
        if (row == null) return null;
        Cell c = row.getCell(colIndex0);
        if (c == null) return null;
        return switch (c.getCellType()) {
            case STRING  -> nullIfBlank(c.getStringCellValue().trim());
            case NUMERIC -> stripDecimal(c.getNumericCellValue());
            case BOOLEAN -> Boolean.toString(c.getBooleanCellValue());
            case FORMULA -> readFormula(c);
            default      -> null;
        };
    }

    public static BigDecimal number(Row row, int colIndex0) {
        if (row == null) return null;
        Cell c = row.getCell(colIndex0);
        if (c == null) return null;
        return switch (c.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(c.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
            case STRING  -> tryParse(c.getStringCellValue());
            case FORMULA -> {
                try { yield BigDecimal.valueOf(c.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP); }
                catch (Exception ignored) { yield tryParse(c.getStringCellValue()); }
            }
            default      -> null;
        };
    }

    private static String readFormula(Cell c) {
        try {
            return switch (c.getCachedFormulaResultType()) {
                case STRING  -> nullIfBlank(c.getStringCellValue().trim());
                case NUMERIC -> stripDecimal(c.getNumericCellValue());
                default -> null;
            };
        } catch (Exception ignored) {
            return null;
        }
    }

    private static BigDecimal tryParse(String s) {
        if (s == null) return null;
        String t = s.trim().replace(",", ".").replaceAll("[^0-9.\\-]", "");
        if (t.isEmpty() || t.equals("-") || t.equals(".")) return null;
        try { return new BigDecimal(t).setScale(2, RoundingMode.HALF_UP); }
        catch (NumberFormatException e) { return null; }
    }

    private static String stripDecimal(double d) {
        if (Math.floor(d) == d && !Double.isInfinite(d)) return Long.toString((long) d);
        return Double.toString(d);
    }

    private static String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}

