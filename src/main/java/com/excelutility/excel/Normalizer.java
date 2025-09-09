package com.excelutility.excel;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles normalization of Excel data, including multi-row headers.
 */
public class Normalizer {

    /**
     * Normalizes data with multi-row headers into a single header row and data rows.
     *
     * @param data          The raw data from the Excel sheet.
     * @param headerRowsCount The number of rows that make up the header.
     * @return A list where the first element is the combined header row, and the rest are data rows.
     */
    public static List<List<Object>> normalizeHeaders(List<List<Object>> data, int headerRowsCount) {
        if (headerRowsCount <= 1) {
            return data; // No normalization needed
        }

        List<List<Object>> normalizedData = new ArrayList<>();
        List<Object> combinedHeader = new ArrayList<>();

        // Get the header rows
        List<List<Object>> headerRows = data.subList(0, headerRowsCount);

        // Find the maximum number of columns
        int maxCols = 0;
        for (List<Object> row : headerRows) {
            if (row.size() > maxCols) {
                maxCols = row.size();
            }
        }

        // Initialize combined header
        for (int i = 0; i < maxCols; i++) {
            combinedHeader.add("");
        }

        // Combine header rows
        for (int col = 0; col < maxCols; col++) {
            StringBuilder headerValue = new StringBuilder();
            for (int row = 0; row < headerRowsCount; row++) {
                List<Object> currentRow = headerRows.get(row);
                if (col < currentRow.size() && currentRow.get(col) != null) {
                    String cellValue = currentRow.get(col).toString().trim();
                    if (!cellValue.isEmpty()) {
                        if (headerValue.length() > 0) {
                            headerValue.append(" ");
                        }
                        headerValue.append(cellValue);
                    }
                }
            }
            combinedHeader.set(col, headerValue.toString());
        }

        normalizedData.add(combinedHeader);

        // Add data rows
        for (int i = headerRowsCount; i < data.size(); i++) {
            normalizedData.add(data.get(i));
        }

        return normalizedData;
    }
}
