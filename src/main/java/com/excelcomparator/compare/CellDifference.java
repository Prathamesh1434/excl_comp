package com.excelcomparator.compare;

/**
 * Represents the differing values of a single cell between two files.
 */
public class CellDifference {
    private final Object value1;
    private final Object value2;

    public CellDifference(Object value1, Object value2) {
        this.value1 = value1;
        this.value2 = value2;
    }

    public Object getValue1() {
        return value1;
    }

    public Object getValue2() {
        return value2;
    }
}
