package com.excelutility.compare;

/**
 * Represents the result of a single cell comparison, holding the values from both files.
 */
public class CellComparisonResult {
    private final Object value1;
    private final Object value2;

    /**
     * Constructs a CellComparisonResult.
     *
     * @param value1 The value from the first file.
     * @param value2 The value from the second file.
     */
    public CellComparisonResult(Object value1, Object value2) {
        this.value1 = value1;
        this.value2 = value2;
    }

    /**
     * Gets the value from the first file.
     *
     * @return The value from file 1.
     */
    public Object getValue1() {
        return value1;
    }

    /**
     * Gets the value from the second file.
     *
     * @return The value from file 2.
     */
    public Object getValue2() {
        return value2;
    }
}
