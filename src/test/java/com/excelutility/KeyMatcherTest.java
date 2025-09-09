package com.excelutility;

import com.excelutility.compare.ComparisonReport;
import com.excelutility.compare.KeyMatcher;
import com.excelutility.compare.RowComparisonStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KeyMatcherTest {

    @Test
    void testMatchAndCompare() {
        // Create sample data for file 1
        List<List<Object>> data1 = new ArrayList<>();
        data1.add(Arrays.asList("ID", "Name", "Value"));
        data1.add(Arrays.asList(1, "John", 100));
        data1.add(Arrays.asList(2, "Jane", 200));
        data1.add(Arrays.asList(3, "Sam", 300));

        // Create sample data for file 2
        List<List<Object>> data2 = new ArrayList<>();
        data2.add(Arrays.asList("ID", "Name", "Value"));
        data2.add(Arrays.asList(1, "John", 100)); // Identical
        data2.add(Arrays.asList(2, "Jane", 250)); // Mismatched
        data2.add(Arrays.asList(4, "Tom", 400)); // Missing in file 1

        // Define key columns
        List<Integer> keyColumns = Arrays.asList(0);

        // Perform comparison
        ComparisonReport report = KeyMatcher.matchAndCompare(data1, data2, keyColumns);

        // Assert results
        long identicalCount = report.getResults().stream().filter(r -> r.getStatus() == RowComparisonStatus.IDENTICAL).count();
        long mismatchedCount = report.getResults().stream().filter(r -> r.getStatus() == RowComparisonStatus.MISMATCHED).count();
        long missingInFile1Count = report.getResults().stream().filter(r -> r.getStatus() == RowComparisonStatus.MISSING_IN_FILE1).count();
        long missingInFile2Count = report.getResults().stream().filter(r -> r.getStatus() == RowComparisonStatus.MISSING_IN_FILE2).count();

        assertEquals(1, identicalCount);
        assertEquals(1, mismatchedCount);
        assertEquals(1, missingInFile1Count);
        assertEquals(1, missingInFile2Count);
    }

    @Test
    void testMatchAndCompareWithCompositeKey() {
        // Create sample data for file 1
        List<List<Object>> data1 = new ArrayList<>();
        data1.add(Arrays.asList("FirstName", "LastName", "Value"));
        data1.add(Arrays.asList("John", "Doe", 100));
        data1.add(Arrays.asList("Jane", "Doe", 200));

        // Create sample data for file 2
        List<List<Object>> data2 = new ArrayList<>();
        data2.add(Arrays.asList("FirstName", "LastName", "Value"));
        data2.add(Arrays.asList("John", "Doe", 100)); // Identical
        data2.add(Arrays.asList("Jane", "Doe", 250)); // Mismatched

        // Define key columns
        List<Integer> keyColumns = Arrays.asList(0, 1);

        // Perform comparison
        ComparisonReport report = KeyMatcher.matchAndCompare(data1, data2, keyColumns);

        // Assert results
        long identicalCount = report.getResults().stream().filter(r -> r.getStatus() == RowComparisonStatus.IDENTICAL).count();
        long mismatchedCount = report.getResults().stream().filter(r -> r.getStatus() == RowComparisonStatus.MISMATCHED).count();

        assertEquals(1, identicalCount);
        assertEquals(1, mismatchedCount);
    }
}
