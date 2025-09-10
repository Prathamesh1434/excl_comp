package com.excelutility.io;

import com.excelutility.core.CellDifference;
import com.excelutility.core.ComparisonResult;
import com.excelutility.core.MismatchType;
import com.excelutility.core.RowComparisonStatus;
import com.excelutility.core.RowResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
            Sheet sheet = workbook.createSheet("Comparison Results");

            Map<Object, CellStyle> styleCache = createStyles(workbook);

            Row headerRow = sheet.createRow(0);
            List<String> headers = result.getFinalHeaders();
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
            }

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

            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
            }
        }
    }

    private static Map<Object, CellStyle> createStyles(XSSFWorkbook workbook) {
        Map<Object, CellStyle> styles = new HashMap<>();

        // Mismatch Type Colors (matching ResultTableRenderer)
        styles.put(MismatchType.NUMERIC, createStyleWithColor(workbook, new java.awt.Color(255, 255, 153)));
        styles.put(MismatchType.STRING, createStyleWithColor(workbook, new java.awt.Color(255, 179, 179)));
        styles.put(MismatchType.BLANK_VS_NON_BLANK, createStyleWithColor(workbook, new java.awt.Color(255, 204, 153)));
        styles.put(MismatchType.TYPE_MISMATCH, createStyleWithColor(workbook, new java.awt.Color(221, 179, 255)));

        // Row Status Colors
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
}
