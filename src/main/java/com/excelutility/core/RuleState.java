package com.excelutility.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * A serializable representation of a FilterRulePanel for saving to a profile.
 */
public class RuleState {

    private final String name;
    private final String columnName;
    private final Operator operator;
    private final String value;
    private final FilteringService.LogicalOperator connector;
    private final String connectorColor;
    private final long recordCount;

    @JsonCreator
    public RuleState(
            @JsonProperty("name") String name,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("operator") Operator operator,
            @JsonProperty("value") String value,
            @JsonProperty("connector") FilteringService.LogicalOperator connector,
            @JsonProperty("connectorColor") String connectorColor,
            @JsonProperty("recordCount") long recordCount) {
        this.name = name;
        this.columnName = columnName;
        this.operator = operator;
        this.value = value;
        this.connector = connector;
        this.connectorColor = connectorColor;
        this.recordCount = recordCount;
    }

    // Getters
    public String getName() { return name; }
    public String getColumnName() { return columnName; }
    public Operator getOperator() { return operator; }
    public String getValue() { return value; }
    public FilteringService.LogicalOperator getConnector() { return connector; }
    public String getConnectorColor() { return connectorColor; }
    public long getRecordCount() { return recordCount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleState ruleState = (RuleState) o;
        return recordCount == ruleState.recordCount &&
                Objects.equals(name, ruleState.name) &&
                Objects.equals(columnName, ruleState.columnName) &&
                operator == ruleState.operator &&
                Objects.equals(value, ruleState.value) &&
                connector == ruleState.connector &&
                Objects.equals(connectorColor, ruleState.connectorColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, columnName, operator, value, connector, connectorColor, recordCount);
    }
}
