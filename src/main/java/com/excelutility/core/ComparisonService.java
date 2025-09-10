package com.excelutility.core;

import com.excelutility.io.ExcelReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Orchestrates the entire comparison process.
 */
public class ComparisonService {

    public ComparisonResult compare(ComparisonProfile profile) throws Exception {
        // 1. Read data from files
        List<List<Object>> sourceData = ExcelReader.read(profile.getSourceFilePath(), profile.getSourceSheetName(), profile.isUseStreaming());
        List<List<Object>> targetData = ExcelReader.read(profile.getTargetFilePath(), profile.getTargetSheetName(), profile.isUseStreaming());

        // TODO: Apply normalization from profile

        // For now, assume headers are the first row
        List<Object> sourceHeaders = sourceData.get(0);
        List<Object> targetHeaders = targetData.get(0);

        List<List<Object>> sourceRows = sourceData.subList(1, sourceData.size());
        List<List<Object>> targetRows = targetData.subList(1, targetData.size());

        // 2. Perform row matching
        List<RowResult> rowResults = matchRows(sourceRows, targetRows, sourceHeaders, targetHeaders, profile);

        // 3. Create final report
        return new ComparisonResult(sourceHeaders.stream().map(Object::toString).collect(Collectors.toList()), rowResults);
    }

    public List<RowResult> matchRows(List<List<Object>> sourceRows, List<List<Object>> targetRows, List<Object> sourceHeaders, List<Object> targetHeaders, ComparisonProfile profile) {
        List<RowResult> results = new ArrayList<>();

        if (profile.getRowMatchStrategy() == RowMatchStrategy.BY_PRIMARY_KEY) {
            Map<String, List<Object>> targetMap = buildKeyMap(targetRows, targetHeaders, profile.getColumnMappings(), profile.getKeyColumns());

            for (List<Object> sourceRow : sourceRows) {
                String key = buildKey(sourceRow, sourceHeaders, profile.getKeyColumns());
                if (targetMap.containsKey(key)) {
                    List<Object> targetRow = targetMap.get(key);
                    Map<Integer, CellDifference> diffs = compareRowCells(sourceRow, targetRow, sourceHeaders, targetHeaders, profile);

                    if (diffs.isEmpty()) {
                        results.add(new RowResult(RowComparisonStatus.MATCHED_IDENTICAL, key, sourceRow, targetRow, diffs));
                    } else {
                        results.add(new RowResult(RowComparisonStatus.MATCHED_MISMATCHED, key, sourceRow, targetRow, diffs));
                    }
                    targetMap.remove(key);
                } else {
                    results.add(new RowResult(RowComparisonStatus.SOURCE_ONLY, key, sourceRow, null, null));
                }
            }

            for (Map.Entry<String, List<Object>> entry : targetMap.entrySet()) {
                results.add(new RowResult(RowComparisonStatus.TARGET_ONLY, entry.getKey(), null, entry.getValue(), null));
            }
        } else {
            // TODO: Implement other matching strategies
        }
        return results;
    }

    private Map<Integer, CellDifference> compareRowCells(List<Object> sourceRow, List<Object> targetRow, List<Object> sourceHeaders, List<Object> targetHeaders, ComparisonProfile profile) {
        Map<Integer, CellDifference> differences = new HashMap<>();
        Map<String, String> mappings = profile.getColumnMappings();

        for (Map.Entry<String, String> mapping : mappings.entrySet()) {
            String sourceColName = mapping.getKey();
            String targetColName = mapping.getValue();

            int sourceColIndex = sourceHeaders.indexOf(sourceColName);
            int targetColIndex = targetHeaders.indexOf(targetColName);

            if (sourceColIndex == -1 || targetColIndex == -1) continue;

            Object sourceVal = sourceColIndex < sourceRow.size() ? sourceRow.get(sourceColIndex) : null;
            Object targetVal = targetColIndex < targetRow.size() ? targetRow.get(targetColIndex) : null;

            // Apply global normalization rules
            String normSourceVal = normalize(sourceVal, profile);
            String normTargetVal = normalize(targetVal, profile);

            if (!Objects.equals(normSourceVal, normTargetVal)) {
                differences.put(sourceColIndex, new CellDifference(sourceVal, targetVal, normSourceVal, normTargetVal, "Value mismatch"));
            }
        }
        return differences;
    }

    private String normalize(Object value, ComparisonProfile profile) {
        if (value == null) return "";
        String str = value.toString();
        if (profile.isTrimWhitespace()) {
            str = str.trim();
        }
        if (profile.isIgnoreCase()) {
            str = str.toLowerCase();
        }
        return str;
    }

    private String buildKey(List<Object> row, List<Object> headers, List<String> keyColumns) {
        List<String> keyParts = new ArrayList<>();
        for (String keyColumn : keyColumns) {
            int index = headers.indexOf(keyColumn);
            if (index != -1 && index < row.size()) {
                keyParts.add(Objects.toString(row.get(index), ""));
            }
        }
        return String.join("||", keyParts);
    }

    private Map<String, List<Object>> buildKeyMap(List<List<Object>> rows, List<Object> headers, Map<String, String> columnMappings, List<String> keyColumns) {
        Map<String, List<Object>> map = new HashMap<>();
        // Invert mapping to find target key columns
        Map<String, String> targetToSourceMapping = columnMappings.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        for (List<Object> row : rows) {
            List<String> keyParts = new ArrayList<>();
            for (String sourceKeyColumn : keyColumns) {
                String targetKeyColumn = targetToSourceMapping.get(sourceKeyColumn);
                if (targetKeyColumn != null) {
                    int index = headers.indexOf(targetKeyColumn);
                    if (index != -1 && index < row.size()) {
                        keyParts.add(Objects.toString(row.get(index), ""));
                    }
                }
            }
            map.put(String.join("||", keyParts), row);
        }
        return map;
    }
}
