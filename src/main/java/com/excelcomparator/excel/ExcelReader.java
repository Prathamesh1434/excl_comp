package com.excelcomparator.excel;

import org.apache.poi.ss.usermodel.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class to read data from Excel files (.xls and .xlsx).
 */
public class ExcelReader {

    private final Workbook workbook;

    /**
     * Constructs an ExcelReader and loads the workbook from the specified file path.
     *
     * @param filePath The path to the Excel file.
     * @throws IOException If an I/O error occurs while reading the file.
     */
    public ExcelReader(String filePath) throws IOException {
        workbook = WorkbookFactory.create(new File(filePath));
    }

    /**
     * Gets a list of all sheet names in the workbook.
     *
     * @return A list of sheet names.
     */
    public List<String> getSheetNames() {
        List<String> sheetNames = new ArrayList<>();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            sheetNames.add(workbook.getSheetName(i));
        }
        return sheetNames;
    }

    /**
     * Reads data from the specified sheet by name up to a given row limit.
     *
     * @param sheetName The name of the sheet to read.
     * @param rowLimit  The maximum number of rows to read. If -1, all rows are read.
     * @return A list of rows, where each row is a list of cell values.
     */
    public List<List<Object>> getSheetData(String sheetName, int rowLimit) {
        Sheet sheet = workbook.getSheet(sheetName);
        return readSheet(sheet, rowLimit);
    }

    /**
     * Reads all data from the specified sheet by name.
     *
     * @param sheetName The name of the sheet to read.
     * @return A list of rows, where each row is a list of cell values.
     */
    public List<List<Object>> getSheetData(String sheetName) {
        return getSheetData(sheetName, -1);
    }

    /**
     * Helper method to read data from a Sheet object.
     *
     * @param sheet    The Sheet object to read from.
     * @param rowLimit The maximum number of rows to read.
     * @return A list of rows.
     */
    private List<List<Object>> readSheet(Sheet sheet, int rowLimit) {
        if (sheet == null) {
            return new ArrayList<>();
        }

        List<List<Object>> data = new ArrayList<>();
        DataFormatter dataFormatter = new DataFormatter();
        int rowCount = 0;

        for (Row row : sheet) {
            if (rowLimit > 0 && rowCount >= rowLimit) {
                break;
            }
            List<Object> rowData = new ArrayList<>();
            for (int i = 0; i < row.getLastCellNum(); i++) {
                Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
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
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                            rowData.add(dateFormat.format(cell.getDateCellValue()));
                        } else {
                            rowData.add(dataFormatter.formatCellValue(cell));
                        }
                        break;
                    case BOOLEAN:
                        rowData.add(cell.getBooleanCellValue());
                        break;
                    case FORMULA:
                        rowData.add(dataFormatter.formatCellValue(cell));
                        break;
                    default:
                        rowData.add("");
                }
            }
            data.add(rowData);
            rowCount++;
        }
        return data;
    }

    /**
     * Closes the workbook to release resources.
     *
     * @throws IOException If an error occurs while closing the workbook.
     */
    public void close() throws IOException {
        if (workbook != null) {
            workbook.close();
        }
    }
}
