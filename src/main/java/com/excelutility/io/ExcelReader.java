package com.excelutility.io;

import org.apache.poi.ss.usermodel.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads data from an Excel file.
 * The streaming logic has been removed in favor of the more robust in-memory reader
 * to ensure correctness and prevent data misalignment issues with empty cells.
 */
public class ExcelReader {

    /**
     * Reads the data from a specified sheet in an Excel file.
     *
     * @param filePath     The path to the Excel file.
     * @param sheetName    The name of the sheet to read.
     * @param useStreaming This parameter is now ignored. The robust in-memory reader is always used.
     * @return A list of lists representing the rows and cells of the sheet.
     * @throws IOException if an I/O error occurs.
     */
    public static List<List<Object>> read(String filePath, String sheetName, boolean useStreaming) throws IOException {
        // Always use the robust in-memory reader to guarantee data integrity.
        return readInMemory(filePath, sheetName);
    }

    private static List<List<Object>> readInMemory(String filePath, String sheetName) throws IOException {
        List<List<Object>> data = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(new File(filePath))) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet '" + sheetName + "' not found in the workbook.");
            }
            DataFormatter dataFormatter = new DataFormatter();

            int maxCols = 0;
            for (Row row : sheet) {
                if (row.getLastCellNum() > maxCols) {
                    maxCols = row.getLastCellNum();
                }
            }

            for (Row row : sheet) {
                List<Object> rowData = new ArrayList<>();
                for (int i = 0; i < maxCols; i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (cell == null) {
                        rowData.add("");
                    } else {
                        rowData.add(dataFormatter.formatCellValue(cell));
                    }
                }
                data.add(rowData);
            }
        }
        return data;
    }

    public static List<String> getSheetNames(String filePath) throws IOException {
        List<String> sheetNames = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(new File(filePath))) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                sheetNames.add(workbook.getSheetName(i));
            }
        }
        return sheetNames;
    }

    public static List<List<Object>> readPreview(String filePath, String sheetName, int rowLimit) throws IOException {
        List<List<Object>> data = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IOException("Sheet '" + sheetName + "' not found in the workbook.");
            }

            int lastRow = Math.min(sheet.getLastRowNum(), rowLimit - 1);
            if (lastRow < 0) return data;

            int maxCols = 0;
            for (int i = 0; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row != null && row.getLastCellNum() > maxCols) {
                    maxCols = row.getLastCellNum();
                }
            }

            for (int i = 0; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                List<Object> rowData = new ArrayList<>();
                for (int j = 0; j < maxCols; j++) {
                    if (row == null) {
                        rowData.add("");
                        continue;
                    }
                    Cell cell = row.getCell(j, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (cell == null) {
                        rowData.add("");
                        continue;
                    }
                    switch (cell.getCellType()) {
                        case STRING:
                            rowData.add(cell.getStringCellValue());
                            break;
                        case NUMERIC:
                            if (DateUtil.isCellDateFormatted(cell)) {
                                rowData.add(cell.getDateCellValue().toString());
                            } else {
                                rowData.add(new DataFormatter().formatCellValue(cell));
                            }
                            break;
                        case BOOLEAN:
                            rowData.add(cell.getBooleanCellValue());
                            break;
                        case FORMULA:
                             try {
                                rowData.add(new DataFormatter().formatCellValue(cell, workbook.getCreationHelper().createFormulaEvaluator()));
                            } catch (Exception e) {
                                rowData.add("!FORMULA_ERROR!");
                            }
                            break;
                        case BLANK:
                            rowData.add("");
                            break;
                        default:
                            rowData.add("!UNSUPPORTED_TYPE!");
                    }
                }
                data.add(rowData);
            }
        }
        return data;
    }
}
