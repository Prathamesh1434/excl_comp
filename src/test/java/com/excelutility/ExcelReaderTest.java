package com.excelutility;

import com.excelutility.excel.ExcelReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelReaderTest {

    private static final String TEST_FILE_PATH = "src/main/resources/testdata/sample.xlsx";

    @BeforeAll
    static void setUp() throws IOException {
        // Create a dummy Excel file for testing
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet1 = workbook.createSheet("Sheet1");
        Row headerRow = sheet1.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("Name");

        Row dataRow1 = sheet1.createRow(1);
        dataRow1.createCell(0).setCellValue(1);
        dataRow1.createCell(1).setCellValue("John Doe");

        Sheet sheet2 = workbook.createSheet("Sheet2");
        Row sheet2Row = sheet2.createRow(0);
        sheet2Row.createCell(0).setCellValue("Data");

        File testDataDir = new File("src/main/resources/testdata");
        if (!testDataDir.exists()) {
            testDataDir.mkdirs();
        }

        try (FileOutputStream fileOut = new FileOutputStream(TEST_FILE_PATH)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }

    @Test
    void testGetSheetNames() throws IOException {
        ExcelReader reader = new ExcelReader(TEST_FILE_PATH);
        List<String> sheetNames = reader.getSheetNames();
        assertNotNull(sheetNames);
        assertEquals(2, sheetNames.size());
        assertTrue(sheetNames.contains("Sheet1"));
        assertTrue(sheetNames.contains("Sheet2"));
        reader.close();
    }

    @Test
    void testGetSheetData() throws IOException {
        ExcelReader reader = new ExcelReader(TEST_FILE_PATH);
        List<List<Object>> data = reader.getSheetData("Sheet1");
        assertNotNull(data);
        assertEquals(2, data.size()); // Header + 1 data row

        // Check header
        assertEquals("ID", data.get(0).get(0));
        assertEquals("Name", data.get(0).get(1));

        // Check data row
        assertEquals("1", data.get(1).get(0).toString());
        assertEquals("John Doe", data.get(1).get(1));

        reader.close();
    }
}
