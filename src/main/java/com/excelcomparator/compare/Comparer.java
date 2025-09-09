package com.excelcomparator.compare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The core engine for comparing two datasets.
 */
public class Comparer {

    public static ComparisonResult compare(List<List<Object>> data1, List<List<Object>> data2, Map<Integer, Integer> keyMapping, boolean columnLevel, boolean rowPresence) {
        List<Object> headers1 = data1.get(0);
        List<RowResult> results = new ArrayList<>();
        Map<String, List<Object>> map1 = buildKeyMap(data1.subList(1, data1.size()), new ArrayList<>(keyMapping.keySet()));

        for (List<Object> row2 : data2.subList(1, data2.size())) {
            String key2 = buildKey(row2, new ArrayList<>(keyMapping.values()));
            if (map1.containsKey(key2)) {
                List<Object> row1 = map1.get(key2);
                Map<Integer, CellDifference> diffs = new HashMap<>();
                if (columnLevel) {
                    diffs = compareRows(row1, row2, keyMapping);
                }

                if (diffs.isEmpty()) {
                    results.add(new RowResult(RowComparisonStatus.IDENTICAL, row1, diffs));
                } else {
                    results.add(new RowResult(RowComparisonStatus.MISMATCHED, row1, diffs));
                }
                map1.remove(key2);
            } else if (rowPresence) {
                results.add(new RowResult(RowComparisonStatus.MISSING_IN_FILE1, row2, null));
            }
        }

        if (rowPresence) {
            for (List<Object> row1 : map1.values()) {
                results.add(new RowResult(RowComparisonStatus.MISSING_IN_FILE2, row1, null));
            }
        }

        return new ComparisonResult(headers1, results);
    }

    private static Map<String, List<Object>> buildKeyMap(List<List<Object>> data, List<Integer> keyColumns) {
        Map<String, List<Object>> map = new HashMap<>();
        for (List<Object> row : data) {
            String key = buildKey(row, keyColumns);
            map.put(key, row);
        }
        return map;
    }

    private static String buildKey(List<Object> row, List<Integer> keyColumns) {
        StringBuilder key = new StringBuilder();
        for (int colIndex : keyColumns) {
            if (colIndex < row.size() && row.get(colIndex) != null) {
                key.append(row.get(colIndex).toString());
            }
            key.append("||");
        }
        return key.toString();
    }

    private static Map<Integer, CellDifference> compareRows(List<Object> row1, List<Object> row2, Map<Integer, Integer> keyMapping) {
        Map<Integer, CellDifference> diff = new HashMap<>();
        int maxCols = Math.max(row1.size(), row2.size());
        for (int i = 0; i < maxCols; i++) {
            // Skip key columns from comparison
            if (keyMapping.containsKey(i)) {
                continue;
            }
            Object val1 = i < row1.size() ? row1.get(i) : "";
            Object val2 = i < row2.size() ? row2.get(i) : "";
            if (!Objects.equals(val1, val2)) {
                diff.put(i, new CellDifference(val1, val2));
            }
        }
        return diff;
    }
}
