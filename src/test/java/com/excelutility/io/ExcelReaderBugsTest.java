package com.excelutility.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class ExcelReaderBugsTest {

    private String testFilePath = "target/test-files/bug_test.xlsx";

    @BeforeEach
    void setUp() throws IOException {
        new File(testFilePath).getParentFile().mkdirs();

        // Create a test file with a blank cell in the middle
        List<List<Object>> data = new ArrayList<>();
        data.add(Arrays.asList("Id", "name", "add"));
        data.add(Arrays.asList(1, "Prath", "Ytl"));
        // This row has a blank cell for 'name'
        List<Object> rowWithBlank = new ArrayList<>();
        rowWithBlank.add(2);
        rowWithBlank.add(null); // Represents the blank cell
        rowWithBlank.add("Nanded");
        data.add(rowWithBlank);

        SimpleExcelWriter.write(data, "Sheet1", testFilePath);
    }

    @AfterEach
    void tearDown() {
        new File(testFilePath).delete();
    }

    @Test
    void testStreamingRead_handlesBlankCellsCorrectly() throws Exception {
        // Use the streaming reader which is suspected to have the bug
        List<List<Object>> result = ExcelReader.read(testFilePath, "Sheet1", true);

        // There should be 3 rows (header + 2 data rows)
        assertEquals(3, result.size(), "Should read all three rows.");

        // Check the third row (index 2) which has the blank cell
        List<Object> rowWithBlank = result.get(2);

        // The row should have 3 columns, with an empty string for the blank cell
        assertEquals(3, rowWithBlank.size(), "Row with blank cell should have the correct number of columns.");

        // Verify the content of the row
        assertEquals(2.0, Double.parseDouble(rowWithBlank.get(0).toString()), "ID should be correct.");
        assertEquals("", rowWithBlank.get(1).toString(), "The 'name' column should be an empty string.");
        assertEquals("Nanded", rowWithBlank.get(2).toString(), "The 'add' column should be correct.");
    }
}
