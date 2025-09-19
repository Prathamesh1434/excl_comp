package com.excelutility.gui;

import com.excelutility.core.FilterRule;
import com.excelutility.core.FilteringService;
import com.excelutility.core.expression.FilterExpression;
import com.excelutility.core.expression.GroupNode;
import com.excelutility.core.expression.RuleNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

public class LogicalGroupPanelTest {

    private LogicalGroupPanel groupPanel;

    @BeforeEach
    void setUp() {
        // The delete listener can be null for testing purposes
        groupPanel = new LogicalGroupPanel("Test Group", null, null);
    }

    private FilterRulePanel createRealRulePanel(String ruleName) {
        FilterRule rule = new FilterRule(FilterRule.SourceType.BY_VALUE, ruleName, "ColumnA", false);
        // The delete listener can be null for this test
        return new FilterRulePanel(ruleName, rule, null);
    }

    @Test
    void testGetExpression_singleRule() {
        FilterRulePanel rulePanel1 = createRealRulePanel("Rule1");
        groupPanel.addComponent(rulePanel1);

        FilterExpression expression = groupPanel.getExpression();

        assertTrue(expression instanceof RuleNode, "Expression should be a RuleNode for a single rule");
        assertEquals("Rule1", expression.getDescriptiveName());
    }

    @Test
    void testGetExpression_twoRules_defaultAnd() {
        FilterRulePanel rulePanel1 = createRealRulePanel("Rule1");
        FilterRulePanel rulePanel2 = createRealRulePanel("Rule2");
        groupPanel.addComponent(rulePanel1);
        groupPanel.addComponent(rulePanel2); // This will add an OperatorPanel between them

        FilterExpression expression = groupPanel.getExpression();

        assertTrue(expression instanceof GroupNode, "Expression should be a GroupNode for multiple rules");
        GroupNode groupNode = (GroupNode) expression;

        // The outer group node is just a wrapper with the name
        assertEquals("Test Group", groupNode.getName());
        assertEquals(1, groupNode.getChildren().size(), "The named group should have one child expression tree");

        GroupNode innerGroup = (GroupNode) groupNode.getChildren().get(0);
        assertEquals(FilteringService.LogicalOperator.AND, innerGroup.getOperator(), "Default operator should be AND");
        assertEquals(2, innerGroup.getChildren().size());
        assertTrue(innerGroup.getChildren().get(0) instanceof RuleNode);
        assertTrue(innerGroup.getChildren().get(1) instanceof RuleNode);
    }
}
