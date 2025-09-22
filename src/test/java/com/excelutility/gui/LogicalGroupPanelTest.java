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
        groupPanel = new LogicalGroupPanel("Test Group", null);
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

        assertTrue(expression instanceof GroupNode, "Expression should be a GroupNode");
        GroupNode groupNode = (GroupNode) expression;
        assertEquals(1, groupNode.getChildren().size(), "Group should contain one child");
        assertTrue(groupNode.getChildren().get(0) instanceof RuleNode, "Child should be a RuleNode");
        assertEquals("ColumnA = 'Rule1'", groupNode.getChildren().get(0).getDescriptiveName());
    }

    @Test
    void testGetExpression_twoRules_defaultAnd() {
        FilterRulePanel rulePanel1 = createRealRulePanel("Rule1");
        FilterRulePanel rulePanel2 = createRealRulePanel("Rule2");
        groupPanel.addComponent(rulePanel1);
        groupPanel.addComponent(rulePanel2);

        FilterExpression expression = groupPanel.getExpression();

        assertTrue(expression instanceof GroupNode, "Expression should be a GroupNode for multiple rules");
        GroupNode groupNode = (GroupNode) expression;

        assertEquals("Test Group", groupNode.getName());
        assertEquals(FilteringService.LogicalOperator.AND, groupNode.getOperator(), "Default operator should be AND");
        assertEquals(2, groupNode.getChildren().size(), "The group should have two children");
        assertTrue(groupNode.getChildren().get(0) instanceof RuleNode);
        assertTrue(groupNode.getChildren().get(1) instanceof RuleNode);
    }
}
