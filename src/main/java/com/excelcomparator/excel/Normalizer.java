package com.excelcomparator.excel;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles normalization of Excel data, including multi-row headers.
 */
public class Normalizer {

    /**
     * Normalizes data with multi-row headers into a single header row and data rows.
     *
     * @param data            The raw data from the Excel sheet.
     * @param headerRowsCount The number of rows that make up the header.
     * @return A list where the first element is the combined header row, and the rest are data rows.
     */
    public static List<List<Object>> normalizeHeaders(List<List<Object>> data, int headerRowsCount) {
        if (headerRowsCount <= 1 || data.size() < headerRowsCount) {
            return data; // No normalization needed or not enough rows
        }

        List<List<Object>> headerRows = data.subList(0, headerRowsCount);
        int maxCols = 0;
        for (List<Object> row : headerRows) {
            maxCols = Math.max(maxCols, row.size());
        }

        // Process headers to handle merged cells
        String[][] processedHeaders = new String[headerRowsCount][maxCols];
        for (int r = 0; r < headerRowsCount; r++) {
            String lastValue = "";
            for (int c = 0; c < maxCols; c++) {
                if (c < headerRows.get(r).size() && headerRows.get(r).get(c) != null && !headerRows.get(r).get(c).toString().trim().isEmpty()) {
                    lastValue = headerRows.get(r).get(c).toString().trim();
                }
                processedHeaders[r][c] = lastValue;
            }
        }

        // Combine the processed header rows
        List<Object> combinedHeader = new ArrayList<>();
        for (int c = 0; c < maxCols; c++) {
            List<String> parts = new ArrayList<>();
            for (int r = 0; r < headerRowsCount; r++) {
                parts.add(processedHeaders[r][c]);
            }
            combinedHeader.add(String.join(" ", parts));
        }

        List<List<Object>> normalizedData = new ArrayList<>();
        normalizedData.add(combinedHeader);
        normalizedData.addAll(data.subList(headerRowsCount, data.size()));
        return normalizedData;
    }
}
