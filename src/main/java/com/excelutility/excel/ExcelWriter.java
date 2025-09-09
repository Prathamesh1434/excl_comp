package com.excelutility.excel;

import com.excelutility.compare.ComparisonReport;
import com.excelutility.compare.RowComparisonResult;
import com.excelutility.compare.RowComparisonStatus;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * A utility class to write a comparison report to an Excel file.
 */
public class ExcelWriter {

    /**
     * Writes the comparison report to an Excel file, preserving color coding.
     *
     * @param report   The comparison report to write.
     * @param filePath The path to the output Excel file.
     * @throws IOException If an I/O error occurs while writing the file.
     */
    public static void writeReport(ComparisonReport report, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Comparison Result");

            // Create styles
            XSSFCellStyle identicalStyle = (XSSFCellStyle) workbook.createCellStyle();
            identicalStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(255, 255, 255), null));
            identicalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFCellStyle mismatchedStyle = (XSSFCellStyle) workbook.createCellStyle();
            mismatchedStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(255, 255, 153), null)); // Light Yellow
            mismatchedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFCellStyle missingStyle = (XSSFCellStyle) workbook.createCellStyle();
            missingStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(255, 182, 193), null)); // Light Pink
            missingStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFCellStyle mismatchCellStyle = (XSSFCellStyle) workbook.createCellStyle();
            mismatchCellStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(255, 204, 153), null)); // Light Orange
            mismatchCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Write header
            Row headerRow = sheet.createRow(0);
            List<Object> headers = report.getHeaders();
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i) != null ? headers.get(i).toString() : "");
            }

            // Write data rows
            int rowNum = 1;
            for (RowComparisonResult rowResult : report.getResults()) {
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
                CellStyle rowStyle;
                switch (rowResult.getStatus()) {
                    case MISMATCHED:
                        rowStyle = mismatchedStyle;
                        break;
                    case MISSING_IN_FILE1:
                    case MISSING_IN_FILE2:
                        rowStyle = missingStyle;
                        break;
                    default:
                        rowStyle = identicalStyle;
                        break;
                }

                for (int i = 0; i < rowData.size(); i++) {
                    row.getCell(i).setCellStyle(rowStyle);
                }

                if (rowResult.getStatus() == RowComparisonStatus.MISMATCHED) {
                    for (int colIndex : rowResult.getMismatchedCells().keySet()) {
                        if (colIndex < row.getLastCellNum()) {
                           row.getCell(colIndex).setCellStyle(mismatchCellStyle);
                        }
                    }
                }
            }
             for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to file
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
        }
    }
}
