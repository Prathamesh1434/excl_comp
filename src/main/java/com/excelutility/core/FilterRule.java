package com.excelutility.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single filtering rule defined by the user.
 * This is an immutable data class that holds all the information needed to apply one filter.
 */
public class FilterRule {

    private final String columnName;
    private final Operator operator;
    private final String value;

    /**
     * Constructs a new FilterRule.
     *
     * @param columnName     The name of the column in the data file to apply the filter on.
     * @param operator       The comparison operator.
     * @param value          The value to filter by.
     */
    @JsonCreator
    public FilterRule(
            @JsonProperty("columnName") String columnName,
            @JsonProperty("operator") Operator operator,
            @JsonProperty("value") String value) {
        this.columnName = columnName;
        this.operator = operator;
        this.value = value;
    }

    public String getColumnName() {
        return columnName;
    }

    public Operator getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }


    @Override
    public String toString() {
        return String.format("Filter on column '%s' %s '%s'",
                columnName,
                operator.toString(),
                value);
    }

    /**
     * Generates a short, descriptive name for the rule, suitable for display in the UI.
     * @return A descriptive string representation of the rule.
     */
    public String getDescriptiveName() {
        return String.format("%s %s '%s'", columnName, operator.toString(), value);
    }
}
