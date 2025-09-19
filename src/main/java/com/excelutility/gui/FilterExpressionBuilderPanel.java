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
        // The delete listener for a rule removes it from its parent group
        ActionListener deleteListener = e -> {
            FilterRulePanel sourcePanel = (FilterRulePanel) e.getSource();
            targetGroup.removeComponent(sourcePanel);
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
     * @param availableColumns The list of available column names for dropdowns.
     */
    public void rebuildFromState(com.excelutility.core.FilterBuilderState state, java.util.List<String> availableColumns) {
        rootGroup.removeAll();
        com.excelutility.core.AutoNamingService.reset();

        if (state == null || state.getGroups() == null) {
            rootGroup.revalidate();
            rootGroup.repaint();
            return;
        }

        for (com.excelutility.core.GroupState groupState : state.getGroups()) {
            // Create and configure the group panel
            LogicalGroupPanel newGroup = new LogicalGroupPanel(groupState.getName(), createDeleteListener());
            newGroup.setInterGroupConnector(groupState.getInterGroupConnector());
            newGroup.setConnectorColor(groupState.getConnectorColor());
            newGroup.setRecordCount(groupState.getGroupRecordCount());

            // Add rules to the group
            for (com.excelutility.core.RuleState ruleState : groupState.getRules()) {
                FilterRulePanel newRulePanel = new FilterRulePanel(ruleState, createDeleteListenerForRule(newGroup), availableColumns);
                newGroup.addComponent(newRulePanel);
            }

            // Set the intra-group connector
            newGroup.setIntraGroupConnector(groupState.getIntraGroupConnector());

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
        java.util.List<com.excelutility.core.GroupState> groupStates = new java.util.ArrayList<>();
        for (java.awt.Component comp : rootGroup.getComponents()) {
            if (comp instanceof LogicalGroupPanel) {
                groupStates.add(((LogicalGroupPanel) comp).getGroupState());
            }
        }
        return new com.excelutility.core.FilterBuilderState(groupStates);
    }

    private ActionListener createDeleteListener() {
        return event -> {
            LogicalGroupPanel sourceGroup = (LogicalGroupPanel) event.getSource();
            rootGroup.removeComponent(sourceGroup);
            panelProvider.updateFilterResults();
        };
    }

    private ActionListener createDeleteListenerForRule(LogicalGroupPanel group) {
        return e -> {
            FilterRulePanel sourcePanel = (FilterRulePanel) e.getSource();
            group.removeComponent(sourcePanel);
            panelProvider.updateFilterResults();
        };
    }
}
