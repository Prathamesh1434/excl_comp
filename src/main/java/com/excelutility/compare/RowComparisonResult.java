package com.excelutility.compare;

import java.util.List;
import java.util.Map;

/**
 * Represents the result of a single row comparison.
 */
public class RowComparisonResult {
    private final RowComparisonStatus status;
    private final List<Object> rowData;
    private final Map<Integer, CellComparisonResult> mismatchedCells;

    /**
     * Constructs a RowComparisonResult.
     *
     * @param status          The status of the row comparison.
     * @param rowData         The data of the row.
     * @param mismatchedCells A map of mismatched cells, where the key is the column index.
     */
    public RowComparisonResult(RowComparisonStatus status, List<Object> rowData, Map<Integer, CellComparisonResult> mismatchedCells) {
        this.status = status;
        this.rowData = rowData;
        this.mismatchedCells = mismatchedCells;
    }

    /**
     * Gets the status of the row comparison.
     *
     * @return The row comparison status.
     */
    public RowComparisonStatus getStatus() {
        return status;
    }

    /**
     * Gets the data of the row.
     *
     * @return The row data.
     */
    public List<Object> getRowData() {
        return rowData;
    }

    /**
     * Gets the map of mismatched cells.
     *
     * @return The map of mismatched cells.
     */
    public Map<Integer, CellComparisonResult> getMismatchedCells() {
        return mismatchedCells;
    }
}
