package com.excelutility.gui;

import com.excelutility.core.FilterRule;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The main container panel for building a nested filter expression.
 * This panel holds the root logical group and orchestrates the creation and deletion
 * of rules and subgroups.
 */
public class FilterExpressionBuilderPanel extends JPanel {

    private final LogicalGroupPanel rootGroup;
    private final FilterPanel panelProvider;

    public FilterExpressionBuilderPanel(FilterPanel panelProvider) {
        this.panelProvider = panelProvider;
        setLayout(new MigLayout("fill, insets 5", "[grow]"));
        setBorder(BorderFactory.createTitledBorder("Filter Logic Builder"));

        // The root group cannot be deleted, so its delete listener is null.
        rootGroup = new LogicalGroupPanel("Root", null, e -> panelProvider.updateFilterResults());
        add(rootGroup, "growx");

        // The FilterPanel is now responsible for wiring up all buttons.
    }

    /**
     * Adds a new filter rule to a specific group panel.
     * @param targetGroup The LogicalGroupPanel to add the rule to.
     * @param rule The rule to add.
     */
    public void addRuleToGroup(LogicalGroupPanel targetGroup, FilterRule rule) {
        // The delete listener for a rule removes it from its parent group
        ActionListener deleteListener = e -> {
            FilterRulePanel sourcePanel = (FilterRulePanel) e.getSource();
            targetGroup.removeComponent(sourcePanel);
            panelProvider.isDirty = true;
            panelProvider.updateFilterResults();
        };
        String ruleName = com.excelutility.core.AutoNamingService.suggestRuleName();
        FilterRulePanel newRulePanel = new FilterRulePanel(ruleName, rule, deleteListener);

        // Wire up the preview button to the main panel's logic
        // This is a bit of a workaround, but keeps the logic in FilterPanel.
        // A better long-term solution might use an event bus.
        if (newRulePanel.getPreviewButton() != null) {
            newRulePanel.getPreviewButton().addActionListener(e -> panelProvider.previewRule(newRulePanel));
        }

        targetGroup.addComponent(newRulePanel);
    }

    /**
     * @return The root group panel, which is the entry point to the expression tree.
     */
    public LogicalGroupPanel getRootGroup() {
        return rootGroup;
    }

    /**
     * Clears the existing UI and rebuilds it from a saved state.
     * @param state The state to load.
     */
    public void rebuildFromState(com.excelutility.core.FilterBuilderState state) {
        rootGroup.removeAll();
        com.excelutility.core.AutoNamingService.reset();

        for (com.excelutility.core.GroupState groupState : state.getGroups()) {
            // This is a simplified reconstruction. A full implementation would handle nesting.
            ActionListener deleteListener = event -> {
                LogicalGroupPanel sourceGroup = (LogicalGroupPanel) event.getSource();
                rootGroup.removeComponent(sourceGroup);
            };
            LogicalGroupPanel newGroup = new LogicalGroupPanel(groupState.getName(), deleteListener, e -> panelProvider.updateFilterResults());
            for (com.excelutility.core.RuleState ruleState : groupState.getRules()) {
                addRuleToGroup(newGroup, ruleState.toFilterRule());
            }
            rootGroup.addComponent(newGroup);
        }

        rootGroup.revalidate();
        rootGroup.repaint();
    }

    /**
     * Captures the current state of the UI into a serializable object.
     * @return The current state.
     */
    public com.excelutility.core.FilterBuilderState getState() {
        // This is a simplified capture. A full implementation would handle nesting.
        java.util.List<com.excelutility.core.GroupState> groupStates = new java.util.ArrayList<>();
        for (java.awt.Component comp : rootGroup.getComponents()) {
            if (comp instanceof LogicalGroupPanel) {
                LogicalGroupPanel groupPanel = (LogicalGroupPanel) comp;
                java.util.List<com.excelutility.core.RuleState> ruleStates = new java.util.ArrayList<>();
                for (java.awt.Component ruleComp : groupPanel.getComponents()) {
                    if (ruleComp instanceof FilterRulePanel) {
                        FilterRulePanel rulePanel = (FilterRulePanel) ruleComp;
                        FilterRule rule = rulePanel.getRule();
                        ruleStates.add(new com.excelutility.core.RuleState(rule.getSourceType(), rule.getSourceValue(), rule.getTargetColumn(), rule.isTrimWhitespace()));
                    }
                }
                // Note: This simplified version doesn't capture the group's logical operator.
                groupStates.add(new com.excelutility.core.GroupState(groupPanel.getName(), com.excelutility.core.FilteringService.LogicalOperator.AND, ruleStates, new java.util.ArrayList<>()));
            }
        }
        return new com.excelutility.core.FilterBuilderState(groupStates);
    }
}
