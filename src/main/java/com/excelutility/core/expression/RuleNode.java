package com.excelutility.core.expression;

import com.excelutility.core.FilterRule;
import com.excelutility.core.FilteringService;

import java.util.List;

/**
 * A leaf node in the filter expression tree that represents a single FilterRule.
 */
public class RuleNode implements FilterExpression {

    private final FilterRule rule;

    public RuleNode(FilterRule rule) {
        this.rule = rule;
    }

    public FilterRule getRule() {
        return rule;
    }

    @Override
    public boolean evaluate(List<Object> row, List<String> header, FilteringService service) {
        return service.checkRule(row, header, this.rule);
    }
}
