package com.excelutility.excel;

import org.apache.poi.ss.usermodel.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import org.apache.poi.ss.usermodel.DateUtil;


/**
 * A utility class to read data from Excel files (.xls and .xlsx).
 * This class uses Apache POI to handle Excel operations.
 */
public class ExcelReader {

    private Workbook workbook;

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
     * Reads all data from the specified sheet by name.
     * Each row is represented as a list of objects.
     *
     * @param sheetName The name of the sheet to read.
     * @return A list of rows, where each row is a list of cell values.
     */
    public List<List<Object>> getSheetData(String sheetName) {
        Sheet sheet = workbook.getSheet(sheetName);
        return readSheet(sheet);
    }

    /**
     * Reads all data from the specified sheet by index.
     *
     * @param sheetIndex The index of the sheet to read (0-based).
     * @return A list of rows, where each row is a list of cell values.
     */
    public List<List<Object>> getSheetData(int sheetIndex) {
        Sheet sheet = workbook.getSheetAt(sheetIndex);
        return readSheet(sheet);
    }

    /**
     * Helper method to read data from a Sheet object.
     * It iterates through rows and cells, converting cell values to appropriate types.
     *
     * @param sheet The Sheet object to read from.
     * @return A list of rows, where each row is a list of cell values.
     */
    private List<List<Object>> readSheet(Sheet sheet) {
        if (sheet == null) {
            return new ArrayList<>();
        }

        List<List<Object>> data = new ArrayList<>();
        DataFormatter dataFormatter = new DataFormatter();

        for (Row row : sheet) {
            List<Object> rowData = new ArrayList<>();
            for (Cell cell : row) {
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
                    case BLANK:
                        rowData.add("");
                        break;
                    default:
                        rowData.add("");
                }
            }
            data.add(rowData);
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
