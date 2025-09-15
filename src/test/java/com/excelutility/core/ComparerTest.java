package com.excelutility.core;

import com.excelutility.core.ComparisonProfile;
import com.excelutility.core.ComparisonService;
import com.excelutility.core.RowComparisonStatus;
import com.excelutility.core.RowResult;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class ComparerTest {

    @Test
    void testMatchRows() {
        ComparisonService service = new ComparisonService();
        ComparisonProfile profile = new ComparisonProfile();

        List<Object> headers = Arrays.asList("ID", "Name", "Value");
        Map<String, String> mappings = new HashMap<>();
        mappings.put("ID", "ID");
        mappings.put("Name", "Name");
        mappings.put("Value", "Value");
        profile.setColumnMappings(mappings);
        profile.setKeyColumns(List.of("ID"));
        profile.setIgnoreCase(true);
        profile.setTrimWhitespace(true);

        List<List<Object>> sourceRows = new ArrayList<>();
        sourceRows.add(Arrays.asList(1, "  John  ", 100)); // Should be identical after trim
        sourceRows.add(Arrays.asList(2, "Jane", 200));   // Mismatched value
        sourceRows.add(Arrays.asList(3, "Mike", 300));   // Source only

        List<List<Object>> targetRows = new ArrayList<>();
        targetRows.add(Arrays.asList(1, "john", 100));   // Should be identical after case change
        targetRows.add(Arrays.asList(2, "Jane", 250));   // Mismatched value
        targetRows.add(Arrays.asList(4, "Sue", 400));    // Target only

        List<RowResult> results = service.matchRows(sourceRows, targetRows, headers, headers, profile);

        assertEquals(4, results.size());

        long identical = results.stream().filter(r -> r.getStatus() == RowComparisonStatus.MATCHED_IDENTICAL).count();
        long mismatched = results.stream().filter(r -> r.getStatus() == RowComparisonStatus.MATCHED_MISMATCHED).count();
        long sourceOnly = results.stream().filter(r -> r.getStatus() == RowComparisonStatus.SOURCE_ONLY).count();
        long targetOnly = results.stream().filter(r -> r.getStatus() == RowComparisonStatus.TARGET_ONLY).count();

        assertEquals(1, identical);
        assertEquals(1, mismatched);
        assertEquals(1, sourceOnly);
        assertEquals(1, targetOnly);

        RowResult mismatchedRow = results.stream().filter(r -> r.getStatus() == RowComparisonStatus.MATCHED_MISMATCHED).findFirst().get();
        assertTrue(mismatchedRow.getDifferences().containsKey(2)); // Value column
    }

    @Test
    void testMatchRowsWithCompositeKey() {
        ComparisonService service = new ComparisonService();
        ComparisonProfile profile = new ComparisonProfile();

        List<Object> headers = Arrays.asList("FirstName", "LastName", "City");
        Map<String, String> mappings = new HashMap<>();
        mappings.put("FirstName", "FirstName");
        mappings.put("LastName", "LastName");
        mappings.put("City", "City");
        profile.setColumnMappings(mappings);
        profile.setKeyColumns(Arrays.asList("FirstName", "LastName"));

        List<List<Object>> sourceRows = new ArrayList<>();
        sourceRows.add(Arrays.asList("John", "Smith", "New York")); // Match
        sourceRows.add(Arrays.asList("Jane", "Doe", "London"));   // Mismatch City
        sourceRows.add(Arrays.asList("John", "Doe", "Paris"));    // Source Only

        List<List<Object>> targetRows = new ArrayList<>();
        targetRows.add(Arrays.asList("John", "Smith", "New York"));
        targetRows.add(Arrays.asList("Jane", "Doe", "Tokyo"));
        targetRows.add(Arrays.asList("Peter", "Jones", "Sydney"));  // Target Only

        List<RowResult> results = service.matchRows(sourceRows, targetRows, headers, headers, profile);

        assertEquals(4, results.size());
        assertEquals(1, results.stream().filter(r -> r.getStatus() == RowComparisonStatus.MATCHED_IDENTICAL).count());
        assertEquals(1, results.stream().filter(r -> r.getStatus() == RowComparisonStatus.MATCHED_MISMATCHED).count());
        assertEquals(1, results.stream().filter(r -> r.getStatus() == RowComparisonStatus.SOURCE_ONLY).count());
        assertEquals(1, results.stream().filter(r -> r.getStatus() == RowComparisonStatus.TARGET_ONLY).count());

        // Check keys
        assertTrue(results.stream().anyMatch(r -> r.getMatchKey().equals("John||Smith")));
        assertTrue(results.stream().anyMatch(r -> r.getMatchKey().equals("Jane||Doe")));
        assertTrue(results.stream().anyMatch(r -> r.getMatchKey().equals("John||Doe")));
        assertTrue(results.stream().anyMatch(r -> r.getMatchKey().equals("Peter||Jones")));
    }

    @Test
    void testMatchRowsWithDifferentHeaders() {
        ComparisonService service = new ComparisonService();
        ComparisonProfile profile = new ComparisonProfile();

        List<Object> sourceHeaders = Arrays.asList("EmpID", "FullName", "Location");
        List<Object> targetHeaders = Arrays.asList("ID", "Name", "City");

        Map<String, String> mappings = new HashMap<>();
        mappings.put("EmpID", "ID");
        mappings.put("FullName", "Name");
        mappings.put("Location", "City");
        profile.setColumnMappings(mappings);
        profile.setKeyColumns(List.of("EmpID"));

        List<List<Object>> sourceRows = new ArrayList<>();
        sourceRows.add(Arrays.asList(1, "John Smith", "New York"));

        List<List<Object>> targetRows = new ArrayList<>();
        targetRows.add(Arrays.asList(1, "John Smith", "New York"));

        List<RowResult> results = service.matchRows(sourceRows, targetRows, sourceHeaders, targetHeaders, profile);

        assertEquals(1, results.size());
        assertEquals(RowComparisonStatus.MATCHED_IDENTICAL, results.get(0).getStatus());
    }

    @Test
    void testCellDifferenceTypes() {
        ComparisonService service = new ComparisonService();
        ComparisonProfile profile = new ComparisonProfile();

        List<Object> headers = Arrays.asList("ID", "Type", "Blank", "String", "Numeric");
        Map<String, String> mappings = new HashMap<>();
        mappings.put("ID", "ID");
        mappings.put("Type", "Type");
        mappings.put("Blank", "Blank");
        mappings.put("String", "String");
        mappings.put("Numeric", "Numeric");
        profile.setColumnMappings(mappings);
        profile.setKeyColumns(List.of("ID"));
        profile.setIgnoreCase(false);
        profile.setTrimWhitespace(false);

        List<List<Object>> sourceRows = new ArrayList<>();
        sourceRows.add(Arrays.asList(1, "123", "", "abc", 100.0));

        List<List<Object>> targetRows = new ArrayList<>();
        targetRows.add(Arrays.asList(1, "ABC", "Value", "abd", 100.1));

        List<RowResult> results = service.matchRows(sourceRows, targetRows, headers, headers, profile);
        assertEquals(1, results.size());
        assertEquals(RowComparisonStatus.MATCHED_MISMATCHED, results.get(0).getStatus());

        Map<Integer, CellDifference> diffs = results.get(0).getDifferences();
        assertEquals(4, diffs.size());
        assertEquals(MismatchType.TYPE_MISMATCH, diffs.get(1).getMismatchType());
        assertEquals(MismatchType.BLANK_VS_NON_BLANK, diffs.get(2).getMismatchType());
        assertEquals(MismatchType.STRING, diffs.get(3).getMismatchType());
        assertEquals(MismatchType.NUMERIC, diffs.get(4).getMismatchType());
    }

    @Test
    void testIgnoredColumns() {
        ComparisonService service = new ComparisonService();
        ComparisonProfile profile = new ComparisonProfile();

        List<Object> headers = Arrays.asList("ID", "Name", "ValueToIgnore");
        Map<String, String> mappings = new HashMap<>();
        mappings.put("ID", "ID");
        mappings.put("Name", "Name");
        mappings.put("ValueToIgnore", "ValueToIgnore");
        profile.setColumnMappings(mappings);
        profile.setKeyColumns(List.of("ID"));
        profile.setIgnoredColumns(List.of("ValueToIgnore"));

        List<List<Object>> sourceRows = new ArrayList<>();
        sourceRows.add(Arrays.asList(1, "John", 100));

        List<List<Object>> targetRows = new ArrayList<>();
        targetRows.add(Arrays.asList(1, "John", 200)); // Value is different, but should be ignored

        List<RowResult> results = service.matchRows(sourceRows, targetRows, headers, headers, profile);

        assertEquals(1, results.size());
        assertEquals(RowComparisonStatus.MATCHED_IDENTICAL, results.get(0).getStatus());
    }

    @Test
    void testNormalizationOptions() {
        ComparisonService service = new ComparisonService();
        ComparisonProfile profile = new ComparisonProfile();

        List<Object> headers = Arrays.asList("ID", "Name");
        Map<String, String> mappings = new HashMap<>();
        mappings.put("ID", "ID");
        mappings.put("Name", "Name");
        profile.setColumnMappings(mappings);
        profile.setKeyColumns(List.of("ID"));

        List<List<Object>> sourceRows = new ArrayList<>();
        sourceRows.add(Arrays.asList(1, "  John  "));
        sourceRows.add(Arrays.asList(2, "JANE"));

        List<List<Object>> targetRows = new ArrayList<>();
        targetRows.add(Arrays.asList(1, "John"));
        targetRows.add(Arrays.asList(2, "jane"));

        // 1. Test with trim OFF and case-sensitive
        profile.setTrimWhitespace(false);
        profile.setIgnoreCase(false);
        List<RowResult> results1 = service.matchRows(sourceRows, targetRows, headers, headers, profile);
        assertEquals(RowComparisonStatus.MATCHED_MISMATCHED, results1.stream().filter(r -> r.getMatchKey().equals("1")).findFirst().get().getStatus());
        assertEquals(RowComparisonStatus.MATCHED_MISMATCHED, results1.stream().filter(r -> r.getMatchKey().equals("2")).findFirst().get().getStatus());


        // 2. Test with trim ON and case-sensitive
        profile.setTrimWhitespace(true);
        profile.setIgnoreCase(false);
        List<RowResult> results2 = service.matchRows(sourceRows, targetRows, headers, headers, profile);
        assertEquals(RowComparisonStatus.MATCHED_IDENTICAL, results2.stream().filter(r -> r.getMatchKey().equals("1")).findFirst().get().getStatus());
        assertEquals(RowComparisonStatus.MATCHED_MISMATCHED, results2.stream().filter(r -> r.getMatchKey().equals("2")).findFirst().get().getStatus());

        // 3. Test with trim OFF and case-insensitive
        profile.setTrimWhitespace(false);
        profile.setIgnoreCase(true);
        List<RowResult> results3 = service.matchRows(sourceRows, targetRows, headers, headers, profile);
        assertEquals(RowComparisonStatus.MATCHED_MISMATCHED, results3.stream().filter(r -> r.getMatchKey().equals("1")).findFirst().get().getStatus());
        assertEquals(RowComparisonStatus.MATCHED_IDENTICAL, results3.stream().filter(r -> r.getMatchKey().equals("2")).findFirst().get().getStatus());
    }
}
