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
}
