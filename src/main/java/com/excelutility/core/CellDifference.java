package com.excelutility.core;

/**
 * A data class representing the difference between two cells.
 */
public class CellDifference {
    private final Object sourceValue;
    private final Object targetValue;
    private final Object normalizedSourceValue;
    private final Object normalizedTargetValue;
    private final String notes; // e.g., "Within tolerance", "Case mismatch"

    public CellDifference(Object sourceValue, Object targetValue, Object normalizedSourceValue, Object normalizedTargetValue, String notes) {
        this.sourceValue = sourceValue;
        this.targetValue = targetValue;
        this.normalizedSourceValue = normalizedSourceValue;
        this.normalizedTargetValue = normalizedTargetValue;
        this.notes = notes;
    }

    // Getters
    public Object getSourceValue() { return sourceValue; }
    public Object getTargetValue() { return targetValue; }
    public Object getNormalizedSourceValue() { return normalizedSourceValue; }
    public Object getNormalizedTargetValue() { return normalizedTargetValue; }
    public String getNotes() { return notes; }
}
