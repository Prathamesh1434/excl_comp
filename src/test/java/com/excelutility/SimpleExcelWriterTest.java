package com.excelutility;

import com.excelutility.io.ExcelReader;
import com.excelutility.io.SimpleExcelWriter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class SimpleExcelWriterTest {

    @Test
    void testWriteFilteredResults() throws IOException {
        String filePath = "target/test-files/filtered-results-test.xlsx";
        new File(filePath).getParentFile().mkdirs();

        // The input data should only contain the group-specific results.
        // The "Unified" sheet is generated automatically by the writer.
        Map<String, List<List<Object>>> data = new LinkedHashMap<>();

        List<List<Object>> group1Data = new ArrayList<>();
        group1Data.add(List.of("Header1", "Header2"));
        group1Data.add(List.of("Group1Data1", 2));
        group1Data.add(List.of("CommonData", 100));
        data.put("Group 1", group1Data);

        List<List<Object>> group2Data = new ArrayList<>();
        group2Data.add(List.of("Header1", "Header2"));
        group2Data.add(List.of("Group2Data1", 3));
        group2Data.add(List.of("CommonData", 100)); // This is a duplicate row
        data.put("Group 2", group2Data);

        List<List<Object>> emptyGroupData = new ArrayList<>();
        emptyGroupData.add(List.of("Header1", "Header2"));
        data.put("Empty Group", emptyGroupData);

        SimpleExcelWriter.writeFilteredResults(filePath, data, true, Color.YELLOW);

        File file = new File(filePath);
        assertTrue(file.exists());

        try (Workbook workbook = WorkbookFactory.create(file)) {
            // We expect 4 sheets: Group 1, Group 2, Empty Group, and the auto-generated Unified sheet.
            assertEquals(4, workbook.getNumberOfSheets());
            assertEquals("Group 1", workbook.getSheetName(0));
            assertEquals("Group 2", workbook.getSheetName(1));
            assertEquals("Empty Group", workbook.getSheetName(2));
            assertEquals("Unified", workbook.getSheetName(3));

            // Check the unified sheet. It should contain 3 data rows (the duplicate is removed).
            Sheet unifiedSheet = workbook.getSheet("Unified");
            assertNotNull(unifiedSheet);
            assertEquals(3, unifiedSheet.getLastRowNum()); // 3 data rows + 1 header row = 4 rows total, so last row num is 3.

            // Check that a group-specific sheet is colored
            Sheet group1Sheet = workbook.getSheet("Group 1");
            assertNotNull(group1Sheet.getRow(1).getCell(0).getCellStyle());

            // Check that the unified sheet is not colored
            assertNotNull(unifiedSheet.getRow(1).getCell(0).getCellStyle()); // Style should exist
            assertEquals(64, unifiedSheet.getRow(1).getCell(0).getCellStyle().getFillForegroundColor()); // 64 is the index for default/automatic color

            Sheet emptySheet = workbook.getSheet("Empty Group");
            assertEquals("No rows matched the filter criteria.", emptySheet.getRow(1).getCell(0).getStringCellValue());
        }

        file.delete();
    }

    @Test
    void testWrite() throws Exception {
        String filePath = "target/test-files/writer-test.xlsx";
        new File(filePath).getParentFile().mkdirs();
        List<List<Object>> data = new ArrayList<>();
        data.add(List.of("Header1", "Header2"));
        data.add(List.of("Data1", 123.45));

        SimpleExcelWriter.write(data, "TestSheet", filePath);

        File file = new File(filePath);
        assert(file.exists());

        // Read back to verify content
        List<List<Object>> readData = ExcelReader.read(filePath, "TestSheet", false);
        assertNotNull(readData);
        assertEquals(2, readData.size());
        assertEquals("Header1", readData.get(0).get(0));
        assertEquals("123.45", readData.get(1).get(1).toString());

        file.delete();
    }
}
