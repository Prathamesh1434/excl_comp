package com.excelutility.core;

import com.excelutility.io.SimpleExcelWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

        // Create data file with extra whitespace and a row to test column name filtering
        List<List<Object>> data = new ArrayList<>();
        data.add(Arrays.asList("ID", "Name", "City"));
        data.add(Arrays.asList(1, "Alice", "  New York  "));
        data.add(Arrays.asList(2, "Bob", "Los Angeles"));
        data.add(Arrays.asList(3, "Charlie", "New York"));
        data.add(Arrays.asList(4, "David", "  Chicago"));
        data.add(Arrays.asList(5, "Frank", null)); // Empty cell
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
        rules.add(new FilterRule(FilterRule.SourceType.BY_VALUE, "New York", "City", false));

        Map<String, List<List<Object>>> results = filteringService.filter(dataFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY, rules, filterFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY);
        List<List<Object>> filteredRows = results.values().iterator().next();
        assertEquals(2, filteredRows.size()); // Header + 1 row
        assertEquals("Charlie", filteredRows.get(1).get(1));
    }

    @Test
    void testFilterByValueWithTrim() throws Exception {
        List<FilterRule> rules = new ArrayList<>();
        rules.add(new FilterRule(FilterRule.SourceType.BY_VALUE, "New York", "City", true));

        Map<String, List<List<Object>>> results = filteringService.filter(dataFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY, rules, filterFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY);
        List<List<Object>> filteredRows = results.values().iterator().next();
        assertEquals(3, filteredRows.size()); // Header + 2 rows
    }

    @Test
    void testFilterByColumnValues() throws Exception {
        List<FilterRule> rules = new ArrayList<>();
        rules.add(new FilterRule(FilterRule.SourceType.BY_COLUMN, "Cities", "City", true));

        Map<String, List<List<Object>>> results = filteringService.filter(dataFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY, rules, filterFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY);
        List<List<Object>> filteredRows = results.values().iterator().next();
        assertEquals(4, filteredRows.size()); // Header + 3 rows (Alice, Charlie, David)
    }

    @Test
    void testZeroRecordFilter() throws Exception {
        List<FilterRule> rules = new ArrayList<>();
        rules.add(new FilterRule(FilterRule.SourceType.BY_VALUE, "San Francisco", "City", false));

        Map<String, List<List<Object>>> results = filteringService.filter(dataFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY, rules, filterFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY);
        List<List<Object>> filteredRows = results.values().iterator().next();
        assertEquals(1, filteredRows.size()); // Header only
    }

    @Test
    void testFilterByEmptyCell() throws Exception {
        List<FilterRule> rules = new ArrayList<>();
        rules.add(new FilterRule(FilterRule.SourceType.BY_VALUE, "", "City", false));

        Map<String, List<List<Object>>> results = filteringService.filter(dataFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY, rules, filterFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY);
        List<List<Object>> filteredRows = results.values().iterator().next();
        assertEquals(2, filteredRows.size()); // Header + 1 row
        assertEquals("Frank", filteredRows.get(1).get(1));
    }

    @Test
    void testCountMatchesWithTrim() throws Exception {
        FilterRule rule = new FilterRule(FilterRule.SourceType.BY_VALUE, "  New York  ", "City", true);
        int count = filteringService.countMatches(dataFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY, rule, filterFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY);
        assertEquals(2, count);
    }

    @Test
    void testCountMatchesWithoutTrim() throws Exception {
        FilterRule rule = new FilterRule(FilterRule.SourceType.BY_VALUE, "New York", "City", false);
        int count = filteringService.countMatches(dataFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY, rule, filterFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY);
        assertEquals(1, count);
    }
}
