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
import java.util.stream.Collectors;

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
        data.add(Arrays.asList("ID", "Name", "City", "Status"));
        data.add(Arrays.asList(1, "Alice", "  New York  ", "Active"));
        data.add(Arrays.asList(2, "Bob", "Los Angeles", "Inactive"));
        data.add(Arrays.asList(3, "Charlie", "New York", "Active"));
        data.add(Arrays.asList(4, "David", "  Chicago", "Active"));
        data.add(Arrays.asList(5, "Eve", "Chicago", "Female")); // To test BY_COLUMN
        data.add(Arrays.asList(6, "Frank", null, "Active")); // Empty cell
        SimpleExcelWriter.write(data, "Sheet1", dataFilePath);

        // Create filter file (used in some old manual tests, can be ignored for these unit tests)
        List<List<Object>> filterData = new ArrayList<>();
        filterData.add(Arrays.asList("Cities", "Names", "Female"));
        filterData.add(Arrays.asList("New York", "Alice", "Yes"));
        filterData.add(Arrays.asList("Chicago", "David", "No"));
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
        rules.add(new FilterRule(FilterRule.SourceType.BY_VALUE, "Active", "Status", false));

        List<List<Object>> filteredRows = filteringService.filter(dataFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY, rules, FilteringService.LogicalOperator.OR);
        assertEquals(5, filteredRows.size()); // Header + 4 rows
    }

    @Test
    void testFilterByValueWithTrim() throws Exception {
        List<FilterRule> rules = new ArrayList<>();
        rules.add(new FilterRule(FilterRule.SourceType.BY_VALUE, "New York", "City", true));

        List<List<Object>> filteredRows = filteringService.filter(dataFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY, rules, FilteringService.LogicalOperator.OR);
        assertEquals(3, filteredRows.size()); // Header + 2 rows
    }

    @Test
    void testFilterByColumnName() throws Exception {
        List<FilterRule> rules = new ArrayList<>();
        // This should filter where Status == "Female"
        rules.add(new FilterRule(FilterRule.SourceType.BY_COLUMN, "Female", "Status", true));

        List<List<Object>> filteredRows = filteringService.filter(dataFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY, rules, FilteringService.LogicalOperator.OR);
        assertEquals(2, filteredRows.size()); // Header + 1 row
        assertEquals("Eve", filteredRows.get(1).get(1));
    }

    @Test
    void testZeroRecordFilter() throws Exception {
        List<FilterRule> rules = new ArrayList<>();
        rules.add(new FilterRule(FilterRule.SourceType.BY_VALUE, "San Francisco", "City", false));

        List<List<Object>> filteredRows = filteringService.filter(dataFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY, rules, FilteringService.LogicalOperator.OR);
        assertEquals(1, filteredRows.size()); // Header only
    }

    @Test
    void testFilterByEmptyCell() throws Exception {
        List<FilterRule> rules = new ArrayList<>();
        rules.add(new FilterRule(FilterRule.SourceType.BY_VALUE, "", "City", false));

        List<List<Object>> filteredRows = filteringService.filter(dataFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY, rules, FilteringService.LogicalOperator.OR);
        assertEquals(2, filteredRows.size()); // Header + 1 row
        assertEquals("Frank", filteredRows.get(1).get(1));
    }

    @Test
    void testFilterWithOrOperator() throws Exception {
        List<FilterRule> rules = new ArrayList<>();
        // City is "Los Angeles" (Bob) OR Name is "David" (David)
        rules.add(new FilterRule(FilterRule.SourceType.BY_VALUE, "Los Angeles", "City", false));
        rules.add(new FilterRule(FilterRule.SourceType.BY_VALUE, "David", "Name", false));

        List<List<Object>> filteredRows = filteringService.filter(dataFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY, rules, FilteringService.LogicalOperator.OR);

        assertEquals(3, filteredRows.size()); // Header + Bob + David

        // Check that the correct rows were returned, regardless of order
        List<String> names = filteredRows.stream().skip(1).map(row -> (String) row.get(1)).collect(Collectors.toList());
        assertTrue(names.contains("Bob"));
        assertTrue(names.contains("David"));
    }

    @Test
    void testFilterWithAndOperator() throws Exception {
        List<FilterRule> rules = new ArrayList<>();
        // City is "New York" (trimmed) AND Status is "Active"
        rules.add(new FilterRule(FilterRule.SourceType.BY_VALUE, "New York", "City", true));
        rules.add(new FilterRule(FilterRule.SourceType.BY_VALUE, "Active", "Status", false));

        List<List<Object>> filteredRows = filteringService.filter(dataFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY, rules, FilteringService.LogicalOperator.AND);

        assertEquals(3, filteredRows.size()); // Header + Alice + Charlie

        List<String> names = filteredRows.stream().skip(1).map(row -> (String) row.get(1)).collect(Collectors.toList());
        assertTrue(names.contains("Alice"));
        assertTrue(names.contains("Charlie"));
    }

    @Test
    void testFilterWithAndOperatorNoResults() throws Exception {
        List<FilterRule> rules = new ArrayList<>();
        // City is "New York" AND Name is "Bob"
        rules.add(new FilterRule(FilterRule.SourceType.BY_VALUE, "New York", "City", false));
        rules.add(new FilterRule(FilterRule.SourceType.BY_VALUE, "Bob", "Name", false));

        List<List<Object>> filteredRows = filteringService.filter(dataFilePath, "Sheet1", Collections.singletonList(0), ConcatenationMode.LEAF_ONLY, rules, FilteringService.LogicalOperator.AND);

        assertEquals(1, filteredRows.size()); // Header only
    }
}
