package com.excelcomparator.compare;

import java.util.List;
import java.util.Map;

/**
 * Holds the result of a comparison for a single row.
 */
public class RowResult {
    private final RowComparisonStatus status;
    private final List<Object> rowData;
    private final Map<Integer, CellDifference> differences;

    public RowResult(RowComparisonStatus status, List<Object> rowData, Map<Integer, CellDifference> differences) {
        this.status = status;
        this.rowData = rowData;
        this.differences = differences;
    }

    public RowComparisonStatus getStatus() {
        return status;
    }

    public List<Object> getRowData() {
        return rowData;
    }

    public Map<Integer, CellDifference> getDifferences() {
        return differences;
    }
}
