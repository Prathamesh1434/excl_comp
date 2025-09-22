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
        rootGroup = new LogicalGroupPanel("Root", null);
        add(rootGroup, "growx");

        // The FilterPanel is now responsible for wiring up all buttons.
    }

    /**
     * Adds a new filter rule to a specific group panel.
     * @param targetGroup The LogicalGroupPanel to add the rule to.
     * @param rule The rule to add.
     */
    public void addRuleToGroup(LogicalGroupPanel targetGroup, FilterRule rule) {
        String ruleName = com.excelutility.core.AutoNamingService.suggestRuleName();
        addRuleToGroup(targetGroup, rule, ruleName);
    }

    private void addRuleToGroup(LogicalGroupPanel targetGroup, FilterRule rule, String ruleName) {
        ActionListener deleteListener = e -> {
            FilterRulePanel sourcePanel = (FilterRulePanel) e.getSource();
            targetGroup.removeComponent(sourcePanel);
        };
        FilterRulePanel newRulePanel = new FilterRulePanel(ruleName, rule, deleteListener);

        if (panelProvider != null && newRulePanel.getPreviewButton() != null) {
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
        rootGroup.revalidate();
        rootGroup.repaint();
        com.excelutility.core.AutoNamingService.reset();

        if (state == null || state.getGroups() == null) {
            return;
        }

        // The state is designed to have a single root group state that we unpack
        if (!state.getGroups().isEmpty()) {
            com.excelutility.core.GroupState rootGroupState = state.getGroups().get(0);
            rootGroup.setName(rootGroupState.getName());
            rootGroup.setOperator(rootGroupState.getOperator());
            buildGroupPanelFromState(rootGroup, rootGroupState);
        }

        rootGroup.revalidate();
        rootGroup.repaint();
    }

    private void buildGroupPanelFromState(LogicalGroupPanel parentPanel, com.excelutility.core.GroupState groupState) {
        // Add rules to the current panel
        if (groupState.getRules() != null) {
            for (com.excelutility.core.RuleState ruleState : groupState.getRules()) {
                addRuleToGroup(parentPanel, ruleState.toFilterRule(), ruleState.getName());
            }
        }

        // Recursively add subgroups
        if (groupState.getGroups() != null) {
            for (com.excelutility.core.GroupState subGroupState : groupState.getGroups()) {
                ActionListener deleteListener = event -> {
                    LogicalGroupPanel sourceGroup = (LogicalGroupPanel) event.getSource();
                    parentPanel.removeComponent(sourceGroup);
                    panelProvider.updateFilterResults();
                };
                LogicalGroupPanel newGroupPanel = new LogicalGroupPanel(subGroupState.getName(), deleteListener);
                newGroupPanel.setOperator(subGroupState.getOperator());

                // Recursive call
                buildGroupPanelFromState(newGroupPanel, subGroupState);

                parentPanel.addComponent(newGroupPanel);
            }
        }
    }


    /**
     * Captures the current state of the UI into a serializable object.
     * @return The current state.
     */
    public com.excelutility.core.FilterBuilderState getState() {
        com.excelutility.core.GroupState rootGroupState = createGroupStateFromPanel(rootGroup);
        return new com.excelutility.core.FilterBuilderState(java.util.Collections.singletonList(rootGroupState));
    }

    private com.excelutility.core.GroupState createGroupStateFromPanel(LogicalGroupPanel groupPanel) {
        java.util.List<com.excelutility.core.RuleState> ruleStates = new java.util.ArrayList<>();
        java.util.List<com.excelutility.core.GroupState> groupStates = new java.util.ArrayList<>();

        // Iterate over the components of the content panel, not the group panel itself.
        for (Component comp : groupPanel.getContentPanel().getComponents()) {
            if (comp instanceof FilterRulePanel) {
                FilterRulePanel rulePanel = (FilterRulePanel) comp;
                FilterRule rule = rulePanel.getRule();
                ruleStates.add(new com.excelutility.core.RuleState(rulePanel.getName(), rule.getSourceType(), rule.getSourceValue(), rule.getTargetColumn(), rule.isTrimWhitespace()));
            } else if (comp instanceof LogicalGroupPanel) {
                groupStates.add(createGroupStateFromPanel((LogicalGroupPanel) comp));
            }
        }
        return new com.excelutility.core.GroupState(groupPanel.getName(), groupPanel.getOperator(), ruleStates, groupStates);
    }
}
