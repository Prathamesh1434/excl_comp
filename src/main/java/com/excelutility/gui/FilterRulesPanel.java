package com.excelutility.gui;

import com.excelutility.core.FilterRule;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class FilterRulesPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final List<FilterRule> rules = new ArrayList<>();

    public FilterRulesPanel() {
        setLayout(new MigLayout("fill, insets 5", "[grow]", "[grow][]"));
        setBorder(BorderFactory.createTitledBorder("Configured Filters"));

        tableModel = new DefaultTableModel(new String[]{"Filter Type", "Filter Value", "Target Column"}, 0);
        JTable rulesTable = new JTable(tableModel);
        rulesTable.setEnabled(false); // Read-only view
        add(new JScrollPane(rulesTable), "grow, wrap");

        JButton clearButton = new JButton("Clear All Filters");
        add(clearButton, "right");

        clearButton.addActionListener(e -> clearRules());
    }

    public void addRule(FilterRule rule) {
        rules.add(rule);
        Vector<String> row = new Vector<>();
        row.add(rule.getSourceType().toString());
        row.add(rule.getSourceValue());
        row.add(rule.getTargetColumn());
        tableModel.addRow(row);
    }

    public void clearRules() {
        rules.clear();
        tableModel.setRowCount(0);
    }

    public List<FilterRule> getRules() {
        return new ArrayList<>(rules);
    }
}
