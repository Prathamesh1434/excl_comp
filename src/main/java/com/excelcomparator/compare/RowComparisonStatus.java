package com.excelcomparator.compare;

/**
 * Enum representing the status of a row after comparison.
 */
public enum RowComparisonStatus {
    IDENTICAL,
    MISMATCHED,
    MISSING_IN_FILE1, // Present in File 2, but not in File 1
    MISSING_IN_FILE2  // Present in File 1, but not in File 2
}
