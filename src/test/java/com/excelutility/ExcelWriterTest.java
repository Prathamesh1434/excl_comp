package com.excelutility;

import com.excelutility.compare.ComparisonReport;
import com.excelutility.compare.RowComparisonResult;
import com.excelutility.compare.RowComparisonStatus;
import com.excelutility.excel.ExcelReader;
import com.excelutility.excel.ExcelWriter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExcelWriterTest {

    @Test
    void testWriteReport() throws IOException {
        // Create a sample report
        List<Object> headers = Arrays.asList("ID", "Name", "Value");
        List<RowComparisonResult> results = new ArrayList<>();
        results.add(new RowComparisonResult(RowComparisonStatus.IDENTICAL, Arrays.asList(1, "John", 100), Collections.emptyMap()));
        results.add(new RowComparisonResult(RowComparisonStatus.MISMATCHED, Arrays.asList(2, "Jane", 250), Collections.singletonMap(2, null)));
        ComparisonReport report = new ComparisonReport(results, headers);

        // Define file path
        String filePath = "target/test-report.xlsx";
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }

        // Write the report
        ExcelWriter.writeReport(report, filePath);

        // Read the report back
        ExcelReader reader = new ExcelReader(filePath);
        List<List<Object>> data = reader.getSheetData("Comparison Result");
        reader.close();

        // Assertions
        assertEquals(3, data.size()); // Header + 2 data rows
        assertEquals("ID", data.get(0).get(0));
        assertEquals("Name", data.get(0).get(1));
        assertEquals("Value", data.get(0).get(2));

        assertEquals("1", data.get(1).get(0).toString());
        assertEquals("John", data.get(1).get(1));
        assertEquals("100", data.get(1).get(2).toString());

        assertEquals("2", data.get(2).get(0).toString());
        assertEquals("Jane", data.get(2).get(1));
        assertEquals("250", data.get(2).get(2).toString());

        // Clean up
        file.delete();
    }
}
