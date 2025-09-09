package com.excelutility;

import com.excelutility.excel.Normalizer;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NormalizerTest {

    @Test
    void testNormalizeHeaders() {
        // Create sample data with a 2-row header
        List<List<Object>> data = new ArrayList<>();
        List<Object> headerRow1 = new ArrayList<>();
        headerRow1.add("Info");
        headerRow1.add("");
        headerRow1.add("Details");
        data.add(headerRow1);

        List<Object> headerRow2 = new ArrayList<>();
        headerRow2.add("Name");
        headerRow2.add("ID");
        headerRow2.add("Value");
        data.add(headerRow2);

        List<Object> dataRow1 = new ArrayList<>();
        dataRow1.add("John");
        dataRow1.add(1);
        dataRow1.add(100);
        data.add(dataRow1);

        // Normalize the data
        List<List<Object>> normalizedData = Normalizer.normalizeHeaders(data, 2);

        // Check the result
        assertEquals(2, normalizedData.size()); // Header + 1 data row

        // Check combined header
        List<Object> combinedHeader = normalizedData.get(0);
        assertEquals("Info Name", combinedHeader.get(0));
        assertEquals("ID", combinedHeader.get(1));
        assertEquals("Details Value", combinedHeader.get(2));

        // Check data row
        assertEquals("John", normalizedData.get(1).get(0));
    }
}
