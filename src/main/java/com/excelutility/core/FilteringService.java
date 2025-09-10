package com.excelutility.core;

import com.excelutility.io.ExcelReader;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FilteringService {

    public Map<String, List<List<Object>>> filter(String dataFilePath, String sheetName, List<FilterRule> rules, String filterFilePath, String filterSheetName) throws IOException, InvalidFormatException {
        List<List<Object>> allData = ExcelReader.read(dataFilePath, sheetName, true);
        if (allData.isEmpty()) {
            return new HashMap<>();
        }

        List<Object> header = allData.get(0);
        List<List<Object>> dataRows = allData.subList(1, allData.size());

        Map<String, List<List<Object>>> results = new HashMap<>();

        for (FilterRule rule : rules) {
            List<List<Object>> filteredRows = new ArrayList<>();
            filteredRows.add(header); // Add header to each result set

            int targetColIndex = header.indexOf(rule.getTargetColumn());
            if (targetColIndex == -1) {
                System.err.println("Warning: Target column '" + rule.getTargetColumn() + "' not found in data file. Skipping rule.");
                continue;
            }

            if (rule.getSourceType() == FilterRule.SourceType.BY_VALUE) {
                String sourceValue = rule.isTrimWhitespace() ? rule.getSourceValue().trim() : rule.getSourceValue();
                for (List<Object> row : dataRows) {
                    if (targetColIndex < row.size() && row.get(targetColIndex) != null) {
                        String cellValue = row.get(targetColIndex).toString();
                        if (rule.isTrimWhitespace()) {
                            cellValue = cellValue.trim();
                        }
                        if (cellValue.equalsIgnoreCase(sourceValue)) {
                            filteredRows.add(row);
                        }
                    }
                }
            } else if (rule.getSourceType() == FilterRule.SourceType.BY_COLUMN) {
                List<List<Object>> filterValuesData = ExcelReader.read(filterFilePath, filterSheetName, true);
                if (filterValuesData.isEmpty()) continue;

                List<Object> filterHeader = filterValuesData.get(0);
                int filterColIndex = filterHeader.indexOf(rule.getSourceValue());
                if (filterColIndex == -1) continue;

                List<String> filterValues = filterValuesData.stream()
                        .skip(1)
                        .filter(row -> filterColIndex < row.size() && row.get(filterColIndex) != null)
                        .map(row -> {
                            String val = row.get(filterColIndex).toString().toLowerCase();
                            return rule.isTrimWhitespace() ? val.trim() : val;
                        })
                        .collect(Collectors.toList());

                for (List<Object> row : dataRows) {
                    if (targetColIndex < row.size() && row.get(targetColIndex) != null) {
                        String cellValue = row.get(targetColIndex).toString().toLowerCase();
                        if (rule.isTrimWhitespace()) {
                            cellValue = cellValue.trim();
                        }
                        if (filterValues.contains(cellValue)) {
                            filteredRows.add(row);
                        }
                    }
                }
            }

            String filterName = String.format("Filtered_by_%s_on_%s", rule.getSourceValue(), rule.getTargetColumn()).replaceAll("[^a-zA-Z0-9.-]", "_");
            results.put(filterName, filteredRows);
        }

        return results;
    }

    public int countMatches(String dataFilePath, String sheetName, FilterRule rule, String filterFilePath, String filterSheetName) throws IOException, InvalidFormatException {
        List<List<Object>> allData = ExcelReader.read(dataFilePath, sheetName, true);
        if (allData.size() < 2) {
            return 0;
        }

        List<Object> header = allData.get(0);
        List<List<Object>> dataRows = allData.subList(1, allData.size());
        int count = 0;

        int targetColIndex = header.indexOf(rule.getTargetColumn());
        if (targetColIndex == -1) {
            return 0;
        }

        if (rule.getSourceType() == FilterRule.SourceType.BY_VALUE) {
            String sourceValue = rule.isTrimWhitespace() ? rule.getSourceValue().trim() : rule.getSourceValue();
            for (List<Object> row : dataRows) {
                if (targetColIndex < row.size() && row.get(targetColIndex) != null) {
                    String cellValue = row.get(targetColIndex).toString();
                     if (rule.isTrimWhitespace()) {
                        cellValue = cellValue.trim();
                    }
                    if (cellValue.equalsIgnoreCase(sourceValue)) {
                        count++;
                    }
                }
            }
        } else if (rule.getSourceType() == FilterRule.SourceType.BY_COLUMN) {
            List<List<Object>> filterValuesData = ExcelReader.read(filterFilePath, filterSheetName, true);
            if (filterValuesData.isEmpty()) return 0;

            List<Object> filterHeader = filterValuesData.get(0);
            int filterColIndex = filterHeader.indexOf(rule.getSourceValue());
            if (filterColIndex == -1) return 0;

            List<String> filterValues = filterValuesData.stream()
                    .skip(1)
                    .filter(row -> filterColIndex < row.size() && row.get(filterColIndex) != null)
                    .map(row -> {
                        String val = row.get(filterColIndex).toString().toLowerCase();
                        return rule.isTrimWhitespace() ? val.trim() : val;
                    })
                    .collect(Collectors.toList());

            for (List<Object> row : dataRows) {
                if (targetColIndex < row.size() && row.get(targetColIndex) != null) {
                    String cellValue = row.get(targetColIndex).toString().toLowerCase();
                    if (rule.isTrimWhitespace()) {
                        cellValue = cellValue.trim();
                    }
                    if (filterValues.contains(cellValue)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
