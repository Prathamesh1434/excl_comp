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
        rootGroup = new LogicalGroupPanel(null);
        add(rootGroup, "growx");

        // Wire up the buttons on the root group
        configureGroupPanel(rootGroup);
    }

    /**
     * Configures the listeners for the buttons within any LogicalGroupPanel.
     * @param groupPanel The panel whose buttons need listeners.
     */
    private void configureGroupPanel(LogicalGroupPanel groupPanel) {
        // Listener for the "Add Group" button
        groupPanel.getAddGroupButton().addActionListener(e -> {
            // The delete listener for a subgroup removes it from its parent (this groupPanel)
            ActionListener deleteListener = event -> {
                LogicalGroupPanel sourceGroup = (LogicalGroupPanel) event.getSource();
                groupPanel.removeComponent(sourceGroup);
            };
            LogicalGroupPanel newGroup = new LogicalGroupPanel(deleteListener);
            configureGroupPanel(newGroup); // Recursively configure the new group's buttons
            groupPanel.addComponent(newGroup);
        });

        // The "Add Rule" button listener will be handled by the parent FilterPanel,
        // which will then call addRuleToGroup(groupPanel, rule).
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
        FilterRulePanel newRulePanel = new FilterRulePanel(rule, panelProvider, deleteListener);
        targetGroup.addComponent(newRulePanel);
    }

    /**
     * @return The root group panel, which is the entry point to the expression tree.
     */
    public LogicalGroupPanel getRootGroup() {
        return rootGroup;
    }
}
