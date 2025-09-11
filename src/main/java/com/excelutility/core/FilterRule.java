package com.excelutility.core;

/**
 * Represents a single filtering rule defined by the user.
 * This is an immutable data class that holds all the information needed to apply one filter.
 */
public class FilterRule {

    /**
     * Defines whether the filter source is a specific cell value or a column name.
     */
    public enum SourceType {
        /**
         * Filter by matching a specific cell's value.
         */
        BY_VALUE,
        /**
         * Filter by using the name of a column as the literal value to match against.
         */
        BY_COLUMN
    }

    private final SourceType sourceType;
    private final String sourceValue;
    private final String targetColumn;
    private final boolean trimWhitespace;

    /**
     * Constructs a new FilterRule.
     *
     * @param sourceType     The type of filter source (BY_VALUE or BY_COLUMN).
     * @param sourceValue    The value to filter by (can be a cell value or a column name).
     * @param targetColumn   The name of the column in the data file to apply the filter on.
     * @param trimWhitespace If true, whitespace will be trimmed from the target column's values before comparison.
     */
    public FilterRule(SourceType sourceType, String sourceValue, String targetColumn, boolean trimWhitespace) {
        this.sourceType = sourceType;
        this.sourceValue = sourceValue;
        this.targetColumn = targetColumn;
        this.trimWhitespace = trimWhitespace;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getSourceValue() {
        return sourceValue;
    }

    public String getTargetColumn() {
        return targetColumn;
    }

    public boolean isTrimWhitespace() {
        return trimWhitespace;
    }

    @Override
    public String toString() {
        return String.format("Filter on column '%s' %s '%s'",
                targetColumn,
                sourceType == SourceType.BY_COLUMN ? "using column name" : "by value",
                sourceValue);
    }
}
