package com.excelutility.io;

import com.excelutility.core.CellDifference;
import com.excelutility.core.ComparisonResult;
import com.excelutility.core.MismatchType;
import com.excelutility.core.RowComparisonStatus;
import com.excelutility.core.RowResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleExcelWriter {

    public static void write(List<List<Object>> data, String sheetName, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            int rowNum = 0;
            for (List<Object> rowData : data) {
                Row row = sheet.createRow(rowNum++);
                int colNum = 0;
                for (Object field : rowData) {
                    Cell cell = row.createCell(colNum++);
                    if (field instanceof String) {
                        cell.setCellValue((String) field);
                    } else if (field instanceof Integer) {
                        cell.setCellValue((Integer) field);
                    } else if (field instanceof Double) {
                        cell.setCellValue((Double) field);
                    } else {
                        cell.setCellValue(field != null ? field.toString() : "");
                    }
                }
            }
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
            }
        }
    }

    public static void writeComparisonResult(ComparisonResult result, String filePath) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Map<Object, CellStyle> styleCache = createStyles(workbook);

            // Create the two sheets
            writeRecordsSheet(workbook, result, styleCache);
            writeSummarySheet(workbook, result.getStats());

            // Write the workbook to the file
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
            }
        }
    }

    private static void writeRecordsSheet(XSSFWorkbook workbook, ComparisonResult result, Map<Object, CellStyle> styleCache) {
        Sheet sheet = workbook.createSheet("Records");

        // Header
        Row headerRow = sheet.createRow(0);
        List<String> headers = result.getFinalHeaders();
        CellStyle headerStyle = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
        }

        // Data Rows
        int rowNum = 1;
        for (RowResult rowResult : result.getRowResults()) {
            Row row = sheet.createRow(rowNum++);
            List<Object> data = rowResult.getStatus() == RowComparisonStatus.TARGET_ONLY
                                ? rowResult.getTargetRowData()
                                : rowResult.getSourceRowData();

            if (data == null) continue;

            CellStyle baseRowStyle = styleCache.get(rowResult.getStatus());

            for (int i = 0; i < data.size(); i++) {
                Cell cell = row.createCell(i);
                Object value = data.get(i);
                cell.setCellValue(value != null ? value.toString() : "");

                CellStyle finalCellStyle = baseRowStyle;
                if (rowResult.getStatus() == RowComparisonStatus.MATCHED_MISMATCHED) {
                    CellDifference diff = rowResult.getDifferences().get(i);
                    if (diff != null && styleCache.containsKey(diff.getMismatchType())) {
                        finalCellStyle = styleCache.get(diff.getMismatchType());
                    }
                }

                if (finalCellStyle != null) {
                    cell.setCellStyle(finalCellStyle);
                }
            }
        }

        for (int i = 0; i < headers.size(); i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static void writeSummarySheet(XSSFWorkbook workbook, ComparisonResult.ComparisonStats stats) {
        Sheet sheet = workbook.createSheet("Summary");

        XSSFFont boldFont = workbook.createFont();
        boldFont.setBold(true);
        CellStyle labelStyle = workbook.createCellStyle();
        labelStyle.setFont(boldFont);

        int rowNum = 0;
        createSummaryRow(sheet, rowNum++, "Total Rows in Source:", stats.totalSourceRows, labelStyle);
        createSummaryRow(sheet, rowNum++, "Total Rows in Target:", stats.totalTargetRows, labelStyle);
        rowNum++;
        createSummaryRow(sheet, rowNum++, "Identical Rows:", stats.matchedIdentical, labelStyle);
        createSummaryRow(sheet, rowNum++, "Mismatched Rows:", stats.matchedMismatched, labelStyle);
        createSummaryRow(sheet, rowNum++, "Extra Rows in Source:", stats.sourceOnly, labelStyle);
        createSummaryRow(sheet, rowNum++, "Extra Rows in Target:", stats.targetOnly, labelStyle);

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private static void createSummaryRow(Sheet sheet, int rowNum, String label, long value, CellStyle labelStyle) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
    }

    private static Map<Object, CellStyle> createStyles(XSSFWorkbook workbook) {
        Map<Object, CellStyle> styles = new HashMap<>();
        styles.put(MismatchType.NUMERIC, createStyleWithColor(workbook, new java.awt.Color(255, 255, 153)));
        styles.put(MismatchType.STRING, createStyleWithColor(workbook, new java.awt.Color(255, 179, 179)));
        styles.put(MismatchType.BLANK_VS_NON_BLANK, createStyleWithColor(workbook, new java.awt.Color(255, 204, 153)));
        styles.put(MismatchType.TYPE_MISMATCH, createStyleWithColor(workbook, new java.awt.Color(221, 179, 255)));
        styles.put(RowComparisonStatus.MATCHED_MISMATCHED, createStyleWithColor(workbook, new java.awt.Color(255, 255, 230)));
        styles.put(RowComparisonStatus.SOURCE_ONLY, createStyleWithColor(workbook, new java.awt.Color(230, 255, 230)));
        styles.put(RowComparisonStatus.TARGET_ONLY, createStyleWithColor(workbook, new java.awt.Color(255, 230, 230)));
        return styles;
    }

    private static XSSFCellStyle createStyleWithColor(XSSFWorkbook workbook, java.awt.Color awtColor) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(awtColor, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    public static void writeFilteredResults(String baseFilePath, Map<String, List<List<Object>>> filteredData, boolean mergeInOneFile, java.awt.Color rowColor) throws IOException {
        if (mergeInOneFile) {
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                for (Map.Entry<String, List<List<Object>>> entry : filteredData.entrySet()) {
                    writeSheet(workbook, entry.getKey(), entry.getValue(), rowColor);
                }
                try (FileOutputStream outputStream = new FileOutputStream(baseFilePath)) {
                    workbook.write(outputStream);
                }
            }
        } else {
            File baseFile = new File(baseFilePath);
            String parentDir = baseFile.getParent();
            String baseName = baseFile.getName();
            String extension = "";
            int i = baseName.lastIndexOf('.');
            if (i > 0) {
                extension = baseName.substring(i);
                baseName = baseName.substring(0, i);
            }

            for (Map.Entry<String, List<List<Object>>> entry : filteredData.entrySet()) {
                String fileName = String.format("%s_%s%s", baseName, entry.getKey(), extension);
                File outputFile = new File(parentDir, fileName);
                try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                    writeSheet(workbook, entry.getKey(), entry.getValue(), rowColor);
                    try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                        workbook.write(outputStream);
                    }
                }
            }
        }
    }

    private static void writeSheet(XSSFWorkbook workbook, String sheetName, List<List<Object>> data, java.awt.Color rowColor) {
        Sheet sheet = workbook.createSheet(sheetName);
        CellStyle rowStyle = createStyleWithColor(workbook, rowColor);

        int rowNum = 0;
        for (List<Object> rowData : data) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;
            for (Object field : rowData) {
                Cell cell = row.createCell(colNum++);
                if (field instanceof String) {
                    cell.setCellValue((String) field);
                } else if (field instanceof Integer) {
                    cell.setCellValue((Integer) field);
                } else if (field instanceof Double) {
                    cell.setCellValue((Double) field);
                } else {
                    cell.setCellValue(field != null ? field.toString() : "");
                }
                if (rowNum > 1) { // Don't color header
                    cell.setCellStyle(rowStyle);
                }
            }
        }

        // Autosize columns
        if (!data.isEmpty()) {
            for (int i = 0; i < data.get(0).size(); i++) {
                sheet.autoSizeColumn(i);
            }
        }
    }
}
