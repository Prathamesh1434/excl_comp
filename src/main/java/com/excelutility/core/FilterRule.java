package com.excelutility.core;

public class FilterRule {

    public enum SourceType {
        BY_VALUE,
        BY_COLUMN
    }

    private final SourceType sourceType;
    private final String sourceValue; // The value or column name from the filter file
    private final String targetColumn; // The column in the data file to apply the filter to
    private final boolean trimWhitespace;

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
        return String.format("Filter %s on column '%s' using value '%s'",
                sourceType == SourceType.BY_COLUMN ? "rows" : "",
                targetColumn,
                sourceValue);
    }
}
