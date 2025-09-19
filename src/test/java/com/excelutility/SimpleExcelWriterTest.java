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

        Map<String, List<List<Object>>> data = new LinkedHashMap<>();
        List<List<Object>> unifiedData = new ArrayList<>();
        unifiedData.add(List.of("Header1", "Header2"));
        unifiedData.add(List.of("UnifiedData1", 1));
        data.put("Unified_Result", unifiedData);

        List<List<Object>> group1Data = new ArrayList<>();
        group1Data.add(List.of("Header1", "Header2"));
        group1Data.add(List.of("Group1Data1", 2));
        data.put("Group_1_TestGroup", group1Data);

        List<List<Object>> emptyGroupData = new ArrayList<>();
        emptyGroupData.add(List.of("Header1", "Header2"));
        data.put("Group_2_EmptyGroup", emptyGroupData);

        SimpleExcelWriter.writeFilteredResults(filePath, data, true, Color.YELLOW);

        File file = new File(filePath);
        assertTrue(file.exists());

        try (Workbook workbook = WorkbookFactory.create(file)) {
            assertEquals(3, workbook.getNumberOfSheets());
            assertEquals("Unified_Result", workbook.getSheetName(0));
            assertEquals("Group_1_TestGroup", workbook.getSheetName(1));
            assertEquals("Group_2_EmptyGroup", workbook.getSheetName(2));

            Sheet unifiedSheet = workbook.getSheetAt(0);
            assertEquals("UnifiedData1", unifiedSheet.getRow(1).getCell(0).getStringCellValue());

            Sheet emptySheet = workbook.getSheetAt(2);
            assertEquals("No rows matched this filter.", emptySheet.getRow(1).getCell(0).getStringCellValue());
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
