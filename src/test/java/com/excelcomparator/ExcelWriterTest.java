package com.excelcomparator;

import com.excelcomparator.compare.ComparisonResult;
import com.excelcomparator.compare.RowResult;
import com.excelcomparator.compare.RowComparisonStatus;
import com.excelcomparator.excel.ExcelReader;
import com.excelcomparator.excel.ExcelWriter;
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
        List<Object> headers = Arrays.asList("ID", "Name");
        List<RowResult> results = new ArrayList<>();
        results.add(new RowResult(RowComparisonStatus.IDENTICAL, Arrays.asList(1, "John"), Collections.emptyMap()));
        ComparisonResult report = new ComparisonResult(headers, results);

        String filePath = "target/test-writer-report.xlsx";
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }

        // Write the report
        ExcelWriter.write(report, filePath);

        // Read it back and verify
        ExcelReader reader = new ExcelReader(filePath);
        List<List<Object>> data = reader.getSheetData("Comparison Report");
        reader.close();

        assertEquals(2, data.size()); // Header + 1 data row
        assertEquals("ID", data.get(0).get(0));
        assertEquals("1", data.get(1).get(0).toString());

        file.delete();
    }
}
