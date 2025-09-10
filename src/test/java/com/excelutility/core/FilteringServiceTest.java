package com.excelutility.core;

import com.excelutility.io.SimpleExcelWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class FilteringServiceTest {

    private FilteringService filteringService;
    private String dataFilePath = "target/test-files/data.xlsx";
    private String filterFilePath = "target/test-files/filters.xlsx";

    @BeforeEach
    void setUp() throws IOException {
        filteringService = new FilteringService();
        new File(dataFilePath).getParentFile().mkdirs();

        // Create data file
        List<List<Object>> data = new ArrayList<>();
        data.add(Arrays.asList("ID", "Name", "City"));
        data.add(Arrays.asList(1, "Alice", "New York"));
        data.add(Arrays.asList(2, "Bob", "Los Angeles"));
        data.add(Arrays.asList(3, "Charlie", "New York"));
        data.add(Arrays.asList(4, "David", "Chicago"));
        SimpleExcelWriter.write(data, "Sheet1", dataFilePath);

        // Create filter file
        List<List<Object>> filterData = new ArrayList<>();
        filterData.add(Arrays.asList("Cities", "Names"));
        filterData.add(Arrays.asList("New York", "Alice"));
        filterData.add(Arrays.asList("Chicago", "David"));
        SimpleExcelWriter.write(filterData, "Sheet1", filterFilePath);
    }

    @AfterEach
    void tearDown() {
        new File(dataFilePath).delete();
        new File(filterFilePath).delete();
    }

    @Test
    void testFilterByValue() throws Exception {
        List<FilterRule> rules = new ArrayList<>();
        rules.add(new FilterRule(FilterRule.SourceType.BY_VALUE, "New York", "City"));

        Map<String, List<List<Object>>> results = filteringService.filter(dataFilePath, "Sheet1", rules, filterFilePath, "Sheet1");

        assertEquals(1, results.size());
        assertTrue(results.containsKey("Filtered_by_New_York_on_City"));
        List<List<Object>> filteredRows = results.get("Filtered_by_New_York_on_City");
        assertEquals(3, filteredRows.size()); // Header + 2 rows
        assertEquals("Alice", filteredRows.get(1).get(1));
        assertEquals("Charlie", filteredRows.get(2).get(1));
    }

    @Test
    void testFilterByColumn() throws Exception {
        List<FilterRule> rules = new ArrayList<>();
        rules.add(new FilterRule(FilterRule.SourceType.BY_COLUMN, "Cities", "City"));

        Map<String, List<List<Object>>> results = filteringService.filter(dataFilePath, "Sheet1", rules, filterFilePath, "Sheet1");

        assertEquals(1, results.size());
        assertTrue(results.containsKey("Filtered_by_Cities_on_City"));
        List<List<Object>> filteredRows = results.get("Filtered_by_Cities_on_City");
        assertEquals(4, filteredRows.size()); // Header + 3 rows
    }
}
