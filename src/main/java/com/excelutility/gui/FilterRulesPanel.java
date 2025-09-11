package com.excelutility.gui;

import com.excelutility.core.FilterRule;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * A panel that displays a list of currently configured filter rules in a table.
 */
public class FilterRulesPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final List<FilterRule> rules = new ArrayList<>();
    private final JTable rulesTable;

    /**
     * Constructs the panel containing the filter rules table and a clear button.
     */
    public FilterRulesPanel() {
        setLayout(new MigLayout("fill, insets 5", "[grow]", "[grow][]"));
        setBorder(BorderFactory.createTitledBorder("Configured Filters"));

        tableModel = new DefaultTableModel(new String[]{"Filter Type", "Filter Value", "Target Column"}, 0);
        rulesTable = new JTable(tableModel);
        rulesTable.setEnabled(false); // Make table read-only

        add(new JScrollPane(rulesTable), "grow, wrap");

        JButton clearButton = new JButton("Clear All Filters");
        add(clearButton, "right");

        clearButton.addActionListener(e -> clearRules());
    }

    /**
     * Adds a new filter rule to the internal list and updates the display table.
     * @param rule The {@link FilterRule} to add.
     */
    public void addRule(FilterRule rule) {
        rules.add(rule);
        Vector<String> row = new Vector<>();
        row.add(rule.getSourceType().toString());
        row.add(rule.getSourceValue());
        row.add(rule.getTargetColumn());
        tableModel.addRow(row);
    }

    /**
     * Removes all filter rules from the list and clears the display table.
     */
    public void clearRules() {
        rules.clear();
        tableModel.setRowCount(0);
    }

    /**
     * Gets a copy of the list of currently configured filter rules.
     * @return A new list containing the active {@link FilterRule} objects.
     */
    public List<FilterRule> getRules() {
        return new ArrayList<>(rules);
    }
}
