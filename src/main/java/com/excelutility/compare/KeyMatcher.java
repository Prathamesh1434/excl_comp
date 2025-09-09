package com.excelutility.compare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Matches rows between two datasets based on key columns and generates a comparison report.
 */
public class KeyMatcher {

    /**
     * Compares two datasets and generates a report.
     *
     * @param data1        The first dataset (list of rows).
     * @param data2        The second dataset (list of rows).
     * @param keyColumns   The indices of the key columns.
     * @return A ComparisonReport.
     */
    public static ComparisonReport matchAndCompare(List<List<Object>> data1, List<List<Object>> data2, List<Integer> keyColumns) {
        List<Object> headers1 = data1.get(0);
        List<RowComparisonResult> results = new ArrayList<>();
        Map<String, List<Object>> map1 = buildKeyMap(data1.subList(1, data1.size()), keyColumns);

        // Process data2
        for (List<Object> row2 : data2.subList(1, data2.size())) {
            String key = buildKey(row2, keyColumns);
            if (map1.containsKey(key)) {
                List<Object> row1 = map1.get(key);
                Map<Integer, CellComparisonResult> diff = RowComparator.compareRows(row1, row2);
                if (diff.isEmpty()) {
                    results.add(new RowComparisonResult(RowComparisonStatus.IDENTICAL, row2, null));
                } else {
                    results.add(new RowComparisonResult(RowComparisonStatus.MISMATCHED, row2, diff));
                }
                map1.remove(key);
            } else {
                results.add(new RowComparisonResult(RowComparisonStatus.MISSING_IN_FILE1, row2, null));
            }
        }

        // Rows left in map1 are missing in file2
        for (List<Object> row1 : map1.values()) {
            results.add(new RowComparisonResult(RowComparisonStatus.MISSING_IN_FILE2, row1, null));
        }

        return new ComparisonReport(results, headers1);
    }

    /**
     * Builds a map from a composite key to a row.
     *
     * @param data       The dataset.
     * @param keyColumns The indices of the key columns.
     * @return A map of key to row data.
     */
    private static Map<String, List<Object>> buildKeyMap(List<List<Object>> data, List<Integer> keyColumns) {
        Map<String, List<Object>> map = new HashMap<>();
        for (List<Object> row : data) {
            String key = buildKey(row, keyColumns);
            map.put(key, row);
        }
        return map;
    }

    /**
     * Builds a composite key string from a row.
     *
     * @param row        The row.
     * @param keyColumns The indices of the key columns.
     * @return The composite key.
     */
    private static String buildKey(List<Object> row, List<Integer> keyColumns) {
        StringBuilder key = new StringBuilder();
        for (int colIndex : keyColumns) {
            if (colIndex < row.size()) {
                key.append(row.get(colIndex).toString());
            }
            key.append("||");
        }
        return key.toString();
    }
}
