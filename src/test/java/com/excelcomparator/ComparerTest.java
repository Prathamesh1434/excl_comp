package com.excelcomparator;

import com.excelcomparator.compare.Comparer;
import com.excelcomparator.compare.ComparisonResult;
import com.excelcomparator.compare.RowComparisonStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ComparerTest {

    @Test
    void testComparer() {
        // Sample Data 1
        List<List<Object>> data1 = new ArrayList<>();
        data1.add(Arrays.asList("ID", "Name", "Value"));
        data1.add(Arrays.asList(1, "John", 100)); // Mismatched
        data1.add(Arrays.asList(2, "Jane", 200)); // Identical
        data1.add(Arrays.asList(3, "Mike", 300)); // Missing in File 2

        // Sample Data 2
        List<List<Object>> data2 = new ArrayList<>();
        data2.add(Arrays.asList("ID", "Name", "Amount"));
        data2.add(Arrays.asList(1, "John", 150)); // Mismatched
        data2.add(Arrays.asList(2, "Jane", 200)); // Identical
        data2.add(Arrays.asList(4, "Sue", 400));  // Missing in File 1

        // Key Mapping (ID column in both files)
        Map<Integer, Integer> keyMapping = new HashMap<>();
        keyMapping.put(0, 0); // File1 Col 0 -> File2 Col 0

        // Perform comparison
        ComparisonResult result = Comparer.compare(data1, data2, keyMapping, true, true);

        // Assertions
        long identicalCount = result.getRowResults().stream().filter(r -> r.getStatus() == RowComparisonStatus.IDENTICAL).count();
        long mismatchedCount = result.getRowResults().stream().filter(r -> r.getStatus() == RowComparisonStatus.MISMATCHED).count();
        long missingInFile1Count = result.getRowResults().stream().filter(r -> r.getStatus() == RowComparisonStatus.MISSING_IN_FILE1).count();
        long missingInFile2Count = result.getRowResults().stream().filter(r -> r.getStatus() == RowComparisonStatus.MISSING_IN_FILE2).count();

        assertEquals(1, identicalCount, "Identical rows count should be 1");
        assertEquals(1, mismatchedCount, "Mismatched rows count should be 1");
        assertEquals(1, missingInFile1Count, "Missing in File 1 count should be 1");
        assertEquals(1, missingInFile2Count, "Missing in File 2 count should be 1");
    }
}
