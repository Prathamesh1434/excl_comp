package com.excelcomparator.compare;

import java.util.List;

/**
 * Encapsulates the entire result of a comparison between two Excel sheets.
 */
public class ComparisonResult {
    private final List<Object> headers;
    private final List<RowResult> rowResults;

    public ComparisonResult(List<Object> headers, List<RowResult> rowResults) {
        this.headers = headers;
        this.rowResults = rowResults;
    }

    public List<Object> getHeaders() {
        return headers;
    }

    public List<RowResult> getRowResults() {
        return rowResults;
    }
}
