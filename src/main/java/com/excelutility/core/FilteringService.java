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
                // Target column not found, skip this rule
                System.err.println("Warning: Target column '" + rule.getTargetColumn() + "' not found in data file. Skipping rule.");
                continue;
            }

            if (rule.getSourceType() == FilterRule.SourceType.BY_VALUE) {
                for (List<Object> row : dataRows) {
                    if (targetColIndex < row.size() && row.get(targetColIndex) != null) {
                        String cellValue = row.get(targetColIndex).toString();
                        if (cellValue.equalsIgnoreCase(rule.getSourceValue())) {
                            filteredRows.add(row);
                        }
                    }
                }
            } else if (rule.getSourceType() == FilterRule.SourceType.BY_COLUMN) {
                // Get all values from the specified column in the filter file
                List<List<Object>> filterValuesData = ExcelReader.read(filterFilePath, filterSheetName, true);
                if (filterValuesData.isEmpty()) continue;

                List<Object> filterHeader = filterValuesData.get(0);
                int filterColIndex = filterHeader.indexOf(rule.getSourceValue());
                if (filterColIndex == -1) continue;

                List<String> filterValues = filterValuesData.stream()
                        .skip(1) // Skip header
                        .filter(row -> filterColIndex < row.size() && row.get(filterColIndex) != null)
                        .map(row -> row.get(filterColIndex).toString().toLowerCase())
                        .collect(Collectors.toList());

                for (List<Object> row : dataRows) {
                    if (targetColIndex < row.size() && row.get(targetColIndex) != null) {
                        String cellValue = row.get(targetColIndex).toString().toLowerCase();
                        if (filterValues.contains(cellValue)) {
                            filteredRows.add(row);
                        }
                    }
                }
            }

            if (filteredRows.size() > 1) { // More than just the header
                String filterName = String.format("Filtered_by_%s_on_%s", rule.getSourceValue(), rule.getTargetColumn()).replaceAll("[^a-zA-Z0-9.-]", "_");
                results.put(filterName, filteredRows);
            }
        }

        return results;
    }
}
