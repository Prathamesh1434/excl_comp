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
        rootGroup = new LogicalGroupPanel("Root", null, panelProvider);
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
        FilterRulePanel newRulePanel = new FilterRulePanel(rule, panelProvider, deleteListener);
        targetGroup.addComponent(newRulePanel);
        panelProvider.calculateCountForRule(rule, newRulePanel);
    }

    /**
     * @return The root group panel, which is the entry point to the expression tree.
     */
    public LogicalGroupPanel getRootGroup() {
        return rootGroup;
    }
}
