package com.excelutility.core;

import com.excelutility.excel.Normalizer;
import com.excelutility.io.ExcelReader;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides the core logic for filtering Excel data based on a set of rules.
 */
public class FilteringService {

    /**
     * Defines the logical operator to use when applying multiple filters.
     */
    public enum LogicalOperator {
        /**
         * A row must match ALL filter rules to be included in the result.
         */
        AND,
        /**
         * A row must match AT LEAST ONE filter rule to be included in the result.
         */
        OR
    }

    /**
     * Filters the data from an Excel sheet based on a list of rules and a logical operator.
     *
     * @param dataFilePath     The path to the Excel file containing the data to be filtered.
     * @param sheetName        The name of the sheet to filter.
     * @param dataHeaderRows   The indices of the header rows in the data file.
     * @param dataConcatMode   The mode for concatenating multi-row headers.
     * @param rules            A list of {@link FilterRule} objects to apply.
     * @param operator         The {@link LogicalOperator} (AND/OR) to use when combining filters.
     * @return A single list of lists representing the filtered data, including the header row.
     * @throws IOException            If there is an error reading the files.
     * @throws InvalidFormatException If the Excel file format is invalid.
     */
    public List<List<Object>> filter(String dataFilePath, String sheetName, List<Integer> dataHeaderRows, ConcatenationMode dataConcatMode, List<FilterRule> rules, LogicalOperator operator) throws IOException, InvalidFormatException {
        // Always use the in-memory reader to ensure accurate handling of null/blank cells.
        List<List<Object>> allData = ExcelReader.read(dataFilePath, sheetName, false);
        if (allData.isEmpty() || rules.isEmpty()) {
            return new ArrayList<>();
        }

        // Build the canonical header from the source file to correctly map column names to indices.
        List<String> header;
        try (Workbook workbook = WorkbookFactory.create(new File(dataFilePath))) {
            Sheet sheet = workbook.getSheet(sheetName);
            header = CanonicalNameBuilder.buildCanonicalHeaders(sheet, dataHeaderRows, dataConcatMode, " | ");
        }

        // Determine where the actual data begins after the header rows.
        int dataStartRow = dataHeaderRows.isEmpty() ? 1 : dataHeaderRows.stream().max(Integer::compareTo).get() + 1;
        List<List<Object>> dataRows = allData.subList(dataStartRow, allData.size());

        List<List<Object>> results = new ArrayList<>();
        results.add(new ArrayList<>(header)); // Start the result set with the header.

        // Iterate through each data row and apply the combined filter logic.
        for (List<Object> row : dataRows) {
            boolean rowMatches;
            if (operator == LogicalOperator.AND) {
                // For AND, every single rule must be true for the row to be a match.
                rowMatches = rules.stream().allMatch(rule -> checkRule(row, header, rule));
            } else { // OR
                // For OR, any single rule can be true for the row to be a match.
                rowMatches = rules.stream().anyMatch(rule -> checkRule(row, header, rule));
            }

            if (rowMatches) {
                results.add(row);
            }
        }

        return results;
    }

    public boolean checkRule(List<Object> row, List<String> header, FilterRule rule) {
        int targetColIndex = header.indexOf(rule.getColumnName());
        if (targetColIndex == -1) {
            return false;
        }

        Object cellObject = (targetColIndex < row.size()) ? row.get(targetColIndex) : null;
        String cellValue = (cellObject == null) ? null : cellObject.toString();
        String ruleValue = rule.getValue();
        Operator operator = rule.getOperator();

        if (operator == Operator.IS_NULL) {
            return cellValue == null || cellValue.trim().isEmpty();
        }
        if (operator == Operator.IS_NOT_NULL) {
            return cellValue != null && !cellValue.trim().isEmpty();
        }

        // For all other operators, if the cell value is null, it cannot be a match.
        if (cellValue == null) {
            return false;
        }

        switch (operator) {
            case EQUALS:
                return cellValue.equalsIgnoreCase(ruleValue);
            case NOT_EQUALS:
                return !cellValue.equalsIgnoreCase(ruleValue);
            case CONTAINS:
                return cellValue.toLowerCase().contains(ruleValue.toLowerCase());
            case NOT_CONTAINS:
                return !cellValue.toLowerCase().contains(ruleValue.toLowerCase());
            case STARTS_WITH:
                return cellValue.toLowerCase().startsWith(ruleValue.toLowerCase());
            case ENDS_WITH:
                return cellValue.toLowerCase().endsWith(ruleValue.toLowerCase());
            case GREATER_THAN:
            case LESS_THAN:
            case GREATER_OR_EQUAL:
            case LESS_OR_EQUAL:
                try {
                    double cellNum = Double.parseDouble(cellValue);
                    double ruleNum = Double.parseDouble(ruleValue);
                    if (operator == Operator.GREATER_THAN) return cellNum > ruleNum;
                    if (operator == Operator.LESS_THAN) return cellNum < ruleNum;
                    if (operator == Operator.GREATER_OR_EQUAL) return cellNum >= ruleNum;
                    if (operator == Operator.LESS_OR_EQUAL) return cellNum <= ruleNum;
                } catch (NumberFormatException e) {
                    return false; // Cannot compare non-numeric values
                }
            // Other cases like REGEX, IN_LIST, BETWEEN would be implemented here.
            default:
                return false;
        }
    }

    /**
     * Counts the number of rows in an Excel sheet that match a single filter rule.
     * This method is designed for providing quick feedback in the UI.
     *
     * @param dataFilePath     The path to the Excel file containing the data.
     * @param sheetName        The name of the sheet to scan.
     * @param dataHeaderRows   The indices of the header rows in the data file.
     * @param dataConcatMode   The mode for concatenating multi-row headers.
     * @param rule             The single {@link FilterRule} to check for matches.
     * @return The total number of matching data rows (header is not counted).
     * @throws IOException            If there is an error reading the file.
     * @throws InvalidFormatException If the Excel file format is invalid.
     */
    public int countMatches(String dataFilePath, String sheetName, List<Integer> dataHeaderRows, ConcatenationMode dataConcatMode, FilterRule rule) throws IOException, InvalidFormatException {
        List<List<Object>> allData = ExcelReader.read(dataFilePath, sheetName, false);
        if (allData.isEmpty()) {
            return 0;
        }

        // Build the canonical header to correctly map column names to indices.
        List<String> header;
        try (Workbook workbook = WorkbookFactory.create(new File(dataFilePath))) {
            Sheet sheet = workbook.getSheet(sheetName);
            header = CanonicalNameBuilder.buildCanonicalHeaders(sheet, dataHeaderRows, dataConcatMode, " | ");
        }

        // Determine where the actual data begins after the header rows.
        int dataStartRow = dataHeaderRows.isEmpty() ? 1 : dataHeaderRows.stream().max(Integer::compareTo).get() + 1;
        List<List<Object>> dataRows = allData.subList(dataStartRow, allData.size());

        int count = 0;
        for (List<Object> row : dataRows) {
            if (checkRule(row, header, rule)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Filters the data from an Excel sheet based on a hierarchical filter expression.
     *
     * @param dataFilePath     The path to the Excel file containing the data to be filtered.
     * @param sheetName        The name of the sheet to filter.
     * @param dataHeaderRows   The indices of the header rows in the data file.
     * @param dataConcatMode   The mode for concatenating multi-row headers.
     * @param expression       The root {@link com.excelutility.core.expression.FilterExpression} node.
     * @return A single list of lists representing the filtered data, including the header row.
     * @throws IOException            If there is an error reading the files.
     * @throws InvalidFormatException If the Excel file format is invalid.
     */
    public List<List<Object>> filter(String dataFilePath, String sheetName, List<Integer> dataHeaderRows, ConcatenationMode dataConcatMode, com.excelutility.core.expression.FilterExpression expression) throws IOException, InvalidFormatException {
        List<List<Object>> allData = ExcelReader.read(dataFilePath, sheetName, false);
        if (allData.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> header;
        try (Workbook workbook = WorkbookFactory.create(new File(dataFilePath))) {
            Sheet sheet = workbook.getSheet(sheetName);
            header = CanonicalNameBuilder.buildCanonicalHeaders(sheet, dataHeaderRows, dataConcatMode, " | ");
        }

        int dataStartRow = dataHeaderRows.isEmpty() ? 1 : dataHeaderRows.stream().max(Integer::compareTo).get() + 1;
        List<List<Object>> dataRows = allData.subList(dataStartRow, allData.size());

        List<List<Object>> results = new ArrayList<>();
        results.add(new ArrayList<>(header));

        for (List<Object> row : dataRows) {
            if (expression.evaluate(row, header, this)) {
                results.add(row);
            }
        }

        return results;
    }

    public java.util.Map<String, List<List<Object>>> filterMultiple(String dataFilePath, String sheetName, List<Integer> dataHeaderRows, ConcatenationMode dataConcatMode, java.util.Map<String, com.excelutility.core.expression.FilterExpression> expressions) throws IOException, org.apache.poi.openxml4j.exceptions.InvalidFormatException {
        // Read the source data once to avoid repeated file access.
        List<List<Object>> allData = ExcelReader.read(dataFilePath, sheetName, false);
        if (allData.isEmpty() || expressions.isEmpty()) {
            return new java.util.LinkedHashMap<>();
        }

        // Prepare the header.
        List<String> header;
        try (Workbook workbook = WorkbookFactory.create(new File(dataFilePath))) {
            Sheet sheet = workbook.getSheet(sheetName);
            header = CanonicalNameBuilder.buildCanonicalHeaders(sheet, dataHeaderRows, dataConcatMode, " | ");
        }
        List<Object> headerObjectList = new ArrayList<>(header);

        // Initialize the results map, with each sheet containing the header row.
        java.util.Map<String, List<List<Object>>> results = new java.util.LinkedHashMap<>();
        for (String name : expressions.keySet()) {
            List<List<Object>> sheetData = new ArrayList<>();
            sheetData.add(headerObjectList);
            results.put(name, sheetData);
        }

        // Determine where the actual data begins.
        int dataStartRow = dataHeaderRows.isEmpty() ? 1 : dataHeaderRows.stream().max(Integer::compareTo).get() + 1;
        List<List<Object>> dataRows = allData.subList(dataStartRow, allData.size());

        // Process each data row against all expressions.
        for (List<Object> row : dataRows) {
            for (java.util.Map.Entry<String, com.excelutility.core.expression.FilterExpression> entry : expressions.entrySet()) {
                String name = entry.getKey();
                com.excelutility.core.expression.FilterExpression expression = entry.getValue();

                if (expression.evaluate(row, header, this)) {
                    results.get(name).add(row);
                }
            }
        }

        return results;
    }
}
