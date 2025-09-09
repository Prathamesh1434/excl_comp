package com.excelutility.compare;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A utility class to compare two rows of data cell by cell.
 */
public class RowComparator {

    /**
     * Compares two rows and returns a map of differences.
     *
     * @param row1 The first row.
     * @param row2 The second row.
     * @return A map where the key is the column index of the mismatch and the value is a CellComparisonResult.
     */
    public static Map<Integer, CellComparisonResult> compareRows(List<Object> row1, List<Object> row2) {
        Map<Integer, CellComparisonResult> diff = new HashMap<>();
        int maxCols = Math.max(row1.size(), row2.size());
        for (int i = 0; i < maxCols; i++) {
            Object val1 = i < row1.size() ? row1.get(i) : "";
            Object val2 = i < row2.size() ? row2.get(i) : "";

            // Treat null and empty strings as equal
            String s1 = val1 == null ? "" : val1.toString();
            String s2 = val2 == null ? "" : val2.toString();

            if (!Objects.equals(s1, s2)) {
                diff.put(i, new CellComparisonResult(val1, val2));
            }
        }
        return diff;
    }
}
