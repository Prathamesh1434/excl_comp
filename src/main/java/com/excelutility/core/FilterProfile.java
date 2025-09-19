package com.excelutility.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * Represents a saved profile for the Filter tool.
 * This class is designed to be serialized to/from JSON.
 */
public class FilterProfile {

    private final String name;
    private final String filePath;
    private final String sheetName;
    private final int headerRow;
    private final List<String> columns;
    private final FilterBuilderState filters;

    @JsonCreator
    public FilterProfile(
            @JsonProperty("name") String name,
            @JsonProperty("filePath") String filePath,
            @JsonProperty("sheetName") String sheetName,
            @JsonProperty("headerRow") int headerRow,
            @JsonProperty("columns") List<String> columns,
            @JsonProperty("filters") FilterBuilderState filters) {
        this.name = name;
        this.filePath = filePath;
        this.sheetName = sheetName;
        this.headerRow = headerRow;
        this.columns = columns;
        this.filters = filters;
    }

    // Getters
    public String getName() { return name; }
    public String getFilePath() { return filePath; }
    public String getSheetName() { return sheetName; }
    public int getHeaderRow() { return headerRow; }
    public List<String> getColumns() { return columns; }
    public FilterBuilderState getFilters() { return filters; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilterProfile that = (FilterProfile) o;
        return headerRow == that.headerRow &&
                Objects.equals(name, that.name) &&
                Objects.equals(filePath, that.filePath) &&
                Objects.equals(sheetName, that.sheetName) &&
                Objects.equals(columns, that.columns) &&
                Objects.equals(filters, that.filters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, filePath, sheetName, headerRow, columns, filters);
    }
}
