package com.excelutility.compare;

import java.util.List;

/**
 * Represents the full report of the comparison between two Excel sheets.
 */
public class ComparisonReport {
    private final List<RowComparisonResult> results;
    private final List<Object> headers;

    /**
     * Constructs a ComparisonReport.
     *
     * @param results The list of row comparison results.
     * @param headers The headers of the compared data.
     */
    public ComparisonReport(List<RowComparisonResult> results, List<Object> headers) {
        this.results = results;
        this.headers = headers;
    }

    /**
     * Gets the list of row comparison results.
     *
     * @return The list of row results.
     */
    public List<RowComparisonResult> getResults() {
        return results;
    }

    /**
     * Gets the headers of the compared data.
     *
     * @return The list of headers.
     */
    public List<Object> getHeaders() {
        return headers;
    }
}
