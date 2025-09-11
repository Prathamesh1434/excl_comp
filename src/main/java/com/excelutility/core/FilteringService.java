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

    public Map<String, List<List<Object>>> filter(String dataFilePath, String sheetName, List<Integer> dataHeaderRows, ConcatenationMode dataConcatMode, List<FilterRule> rules) throws IOException, InvalidFormatException {
        List<List<Object>> allData = ExcelReader.read(dataFilePath, sheetName, false); // Use in-memory reader
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

            String sourceValue = rule.isTrimWhitespace() ? rule.getSourceValue().trim() : rule.getSourceValue();
            for (List<Object> row : dataRows) {
                Object cellObject = (targetColIndex < row.size()) ? row.get(targetColIndex) : null;
                if (isMatch(cellObject, sourceValue, rule.isTrimWhitespace())) {
                    filteredRows.add(row);
                }
            }

            String filterName = String.format("Filtered_by_%s_on_%s", rule.getSourceValue().isEmpty() ? "empty" : rule.getSourceValue(), rule.getTargetColumn()).replaceAll("[^a-zA-Z0-9.-]", "_");
            results.put(filterName, filteredRows);
        }

        return results;
    }

    public int countMatches(String dataFilePath, String sheetName, List<Integer> dataHeaderRows, ConcatenationMode dataConcatMode, FilterRule rule) throws IOException, InvalidFormatException {
        List<List<Object>> allData = ExcelReader.read(dataFilePath, sheetName, false); // Use in-memory reader
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

        String sourceValue = rule.isTrimWhitespace() ? rule.getSourceValue().trim() : rule.getSourceValue();
        for (List<Object> row : dataRows) {
            Object cellObject = (targetColIndex < row.size()) ? row.get(targetColIndex) : null;
            if (isMatch(cellObject, sourceValue, rule.isTrimWhitespace())) {
                count++;
            }
        }
        return count;
    }

    private boolean isMatch(Object cellObject, String sourceValue, boolean trim) {
        String cellValue = (cellObject == null) ? "" : cellObject.toString();
        if (trim) {
            cellValue = cellValue.trim();
        }
        if (sourceValue.isEmpty()) {
            return cellValue.isEmpty();
        }
        return cellValue.equalsIgnoreCase(sourceValue);
    }
}
