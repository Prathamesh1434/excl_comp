package com.excelutility.core.expression;

import com.excelutility.core.FilteringService;

import java.util.ArrayList;
import java.util.List;

/**
 * A branch node in the filter expression tree that represents a logical grouping (AND/OR)
 * of other FilterExpression nodes.
 */
public class GroupNode implements FilterExpression {

    private final FilteringService.LogicalOperator operator;
    private final List<FilterExpression> children = new ArrayList<>();

    public GroupNode(FilteringService.LogicalOperator operator) {
        this.operator = operator;
    }

    public void addChild(FilterExpression child) {
        this.children.add(child);
    }

    public FilteringService.LogicalOperator getOperator() {
        return operator;
    }

    public List<FilterExpression> getChildren() {
        return children;
    }

    @Override
    public boolean evaluate(List<Object> row, List<String> header, FilteringService service) {
        if (children.isEmpty()) {
            return true; // An empty group should not filter anything out
        }

        if (operator == FilteringService.LogicalOperator.AND) {
            // For AND, all children must be true. Return false on the first failure.
            for (FilterExpression child : children) {
                if (!child.evaluate(row, header, service)) {
                    return false;
                }
            }
            return true; // All children passed
        } else { // OR
            // For OR, at least one child must be true. Return true on the first success.
            for (FilterExpression child : children) {
                if (child.evaluate(row, header, service)) {
                    return true;
                }
            }
            return false; // No children passed
        }
    }
}
