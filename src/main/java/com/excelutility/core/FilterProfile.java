package com.excelutility.core;

import com.excelutility.core.expression.FilterExpression;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A data class representing a saved state of the filter configuration,
 * including the entire expression tree.
 */
public class FilterProfile {

    private final FilterExpression rootExpression;

    @JsonCreator
    public FilterProfile(@JsonProperty("rootExpression") FilterExpression rootExpression) {
        this.rootExpression = rootExpression;
    }

    public FilterExpression getRootExpression() {
        return rootExpression;
    }
}
