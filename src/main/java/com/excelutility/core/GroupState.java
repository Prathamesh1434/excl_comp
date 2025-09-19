package com.excelutility.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * A serializable representation of a LogicalGroupPanel for saving to a profile.
 */
public class GroupState {

    private final String name;
    private final FilteringService.LogicalOperator intraGroupConnector;
    private final List<RuleState> rules;
    private final FilteringService.LogicalOperator interGroupConnector;
    private final String connectorColor;
    private final long groupRecordCount;

    @JsonCreator
    public GroupState(
            @JsonProperty("name") String name,
            @JsonProperty("intraGroupConnector") FilteringService.LogicalOperator intraGroupConnector,
            @JsonProperty("rules") List<RuleState> rules,
            @JsonProperty("interGroupConnector") FilteringService.LogicalOperator interGroupConnector,
            @JsonProperty("connectorColor") String connectorColor,
            @JsonProperty("groupRecordCount") long groupRecordCount) {
        this.name = name;
        this.intraGroupConnector = intraGroupConnector;
        this.rules = rules;
        this.interGroupConnector = interGroupConnector;
        this.connectorColor = connectorColor;
        this.groupRecordCount = groupRecordCount;
    }

    // Getters
    public String getName() { return name; }
    public FilteringService.LogicalOperator getIntraGroupConnector() { return intraGroupConnector; }
    public List<RuleState> getRules() { return rules; }
    public FilteringService.LogicalOperator getInterGroupConnector() { return interGroupConnector; }
    public String getConnectorColor() { return connectorColor; }
    public long getGroupRecordCount() { return groupRecordCount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupState that = (GroupState) o;
        return groupRecordCount == that.groupRecordCount &&
                Objects.equals(name, that.name) &&
                intraGroupConnector == that.intraGroupConnector &&
                Objects.equals(rules, that.rules) &&
                interGroupConnector == that.interGroupConnector &&
                Objects.equals(connectorColor, that.connectorColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, intraGroupConnector, rules, interGroupConnector, connectorColor, groupRecordCount);
    }
}
