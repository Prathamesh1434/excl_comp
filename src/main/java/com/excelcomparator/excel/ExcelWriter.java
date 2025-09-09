package com.excelcomparator.excel;

import com.excelcomparator.compare.ComparisonResult;
import com.excelcomparator.compare.RowResult;
import com.excelcomparator.compare.RowComparisonStatus;
import com.excelcomparator.compare.ComparisonResult;
import com.excelcomparator.compare.RowResult;
import com.excelcomparator.compare.RowComparisonStatus;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * A utility class to write a ComparisonResult to an Excel file.
 */
public class ExcelWriter {

    public static void write(ComparisonResult report, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Comparison Report");

            // Cell Styles
            XSSFCellStyle identicalStyle = createStyle(workbook, new Color(255, 255, 255));
            XSSFCellStyle mismatchedStyle = createStyle(workbook, new Color(255, 255, 153)); // Light Yellow
            XSSFCellStyle missingStyle = createStyle(workbook, new Color(255, 182, 193));    // Light Pink
            XSSFCellStyle mismatchCellStyle = createStyle(workbook, new Color(255, 204, 153)); // Light Orange

            // Header
            Row headerRow = sheet.createRow(0);
            List<Object> headers = report.getHeaders();
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i).toString());
            }

            // Data
            int rowNum = 1;
            for (RowResult rowResult : report.getRowResults()) {
                Row row = sheet.createRow(rowNum++);
                List<Object> rowData = rowResult.getRowData();
                for (int i = 0; i < rowData.size(); i++) {
                    Cell cell = row.createCell(i);
                    Object value = rowData.get(i);
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else {
                        cell.setCellValue(value != null ? value.toString() : "");
                    }
                }

                // Apply styles
                applyRowStyles(rowResult, row, identicalStyle, mismatchedStyle, missingStyle, mismatchCellStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to file
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
        }
    }

    private static XSSFCellStyle createStyle(Workbook workbook, Color color) {
        XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(color, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static void applyRowStyles(RowResult rowResult, Row row, CellStyle identical, CellStyle mismatched, CellStyle missing, CellStyle mismatchCell) {
        CellStyle rowStyle;
        switch (rowResult.getStatus()) {
            case MISMATCHED:
                rowStyle = mismatched;
                break;
            case MISSING_IN_FILE1:
            case MISSING_IN_FILE2:
                rowStyle = missing;
                break;
            default:
                rowStyle = identical;
                break;
        }

        for (int i = 0; i < rowResult.getRowData().size(); i++) {
            Cell cell = row.getCell(i);
            if (cell == null) cell = row.createCell(i);
            cell.setCellStyle(rowStyle);
        }

        if (rowResult.getStatus() == RowComparisonStatus.MISMATCHED) {
            for (int colIndex : rowResult.getDifferences().keySet()) {
                Cell cell = row.getCell(colIndex);
                if (cell != null) {
                    cell.setCellStyle(mismatchCell);
                }
            }
        }
    }
}
