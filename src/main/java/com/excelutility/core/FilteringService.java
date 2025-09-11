package com.excelutility.core;

import com.excelutility.io.ExcelReader;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FilteringService {

    public Map<String, List<List<Object>>> filter(String dataFilePath, String sheetName, List<Integer> dataHeaderRows, ConcatenationMode dataConcatMode, List<FilterRule> rules, String filterFilePath, String filterSheetName, List<Integer> filterHeaderRows, ConcatenationMode filterConcatMode) throws IOException, InvalidFormatException {
        List<List<Object>> allData = ExcelReader.read(dataFilePath, sheetName, true);
        if (allData.isEmpty()) {
            return new HashMap<>();
        }

        List<String> header;
        try (Workbook workbook = WorkbookFactory.create(new File(dataFilePath))) {
            Sheet sheet = workbook.getSheet(sheetName);
            header = CanonicalNameBuilder.buildCanonicalHeaders(sheet, dataHeaderRows, dataConcatMode, " | ");
        }

        int dataStartRow = dataHeaderRows.isEmpty() ? 1 : dataHeaderRows.stream().max(Integer::compareTo).get() + 1;
        List<List<Object>> dataRows = allData.subList(dataStartRow, allData.size());

        Map<String, List<List<Object>>> results = new HashMap<>();

        for (FilterRule rule : rules) {
            List<List<Object>> filteredRows = new ArrayList<>();
            filteredRows.add(new ArrayList<>(header));

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
                List<String> filterValues = getFilterValuesFromColumn(filterFilePath, filterSheetName, filterHeaderRows, filterConcatMode, rule);
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

    public int countMatches(String dataFilePath, String sheetName, List<Integer> dataHeaderRows, ConcatenationMode dataConcatMode, FilterRule rule, String filterFilePath, String filterSheetName, List<Integer> filterHeaderRows, ConcatenationMode filterConcatMode) throws IOException, InvalidFormatException {
        List<List<Object>> allData = ExcelReader.read(dataFilePath, sheetName, true);
        if (allData.isEmpty()) return 0;

        List<String> header;
        try (Workbook workbook = WorkbookFactory.create(new File(dataFilePath))) {
            Sheet sheet = workbook.getSheet(sheetName);
            header = CanonicalNameBuilder.buildCanonicalHeaders(sheet, dataHeaderRows, dataConcatMode, " | ");
        }

        int dataStartRow = dataHeaderRows.isEmpty() ? 1 : dataHeaderRows.stream().max(Integer::compareTo).get() + 1;
        List<List<Object>> dataRows = allData.subList(dataStartRow, allData.size());
        int count = 0;

        int targetColIndex = header.indexOf(rule.getTargetColumn());
        if (targetColIndex == -1) return 0;

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
            List<String> filterValues = getFilterValuesFromColumn(filterFilePath, filterSheetName, filterHeaderRows, filterConcatMode, rule);
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

    private List<String> getFilterValuesFromColumn(String filePath, String sheetName, List<Integer> headerRows, ConcatenationMode concatMode, FilterRule rule) throws IOException, InvalidFormatException {
        List<List<Object>> filterValuesData = ExcelReader.read(filePath, sheetName, true);
        if (filterValuesData.isEmpty()) return new ArrayList<>();

        List<String> filterHeader;
        try (Workbook workbook = WorkbookFactory.create(new File(filePath))) {
            Sheet sheet = workbook.getSheet(sheetName);
            filterHeader = CanonicalNameBuilder.buildCanonicalHeaders(sheet, headerRows, concatMode, " | ");
        }

        int filterColIndex = filterHeader.indexOf(rule.getSourceValue());
        if (filterColIndex == -1) return new ArrayList<>();

        int filterDataStartRow = headerRows.isEmpty() ? 1 : headerRows.stream().max(Integer::compareTo).get() + 1;

        return filterValuesData.stream()
                .skip(filterDataStartRow)
                .filter(row -> filterColIndex < row.size() && row.get(filterColIndex) != null)
                .map(row -> {
                    String val = row.get(filterColIndex).toString().toLowerCase();
                    return rule.isTrimWhitespace() ? val.trim() : val;
                })
                .collect(Collectors.toList());
    }
}
