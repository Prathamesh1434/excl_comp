package com.excelutility.gui;

import com.excelutility.core.FilterRule;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class FilterRulesPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final List<FilterRule> rules = new ArrayList<>();
    private final JTable rulesTable;

    public FilterRulesPanel() {
        setLayout(new MigLayout("fill, insets 5", "[grow]", "[grow][]"));
        setBorder(BorderFactory.createTitledBorder("Configured Filters"));

        tableModel = new DefaultTableModel(new String[]{"Filter Type", "Filter Value", "Target Column", "Record Count"}, 0);
        rulesTable = new JTable(tableModel);
        rulesTable.setEnabled(false); // Make table read-only
        rulesTable.getColumnModel().getColumn(3).setCellRenderer(new RecordCountRenderer());

        add(new JScrollPane(rulesTable), "grow, wrap");

        JButton clearButton = new JButton("Clear All Filters");
        add(clearButton, "right");

        clearButton.addActionListener(e -> clearRules());
    }

    public int addRule(FilterRule rule) {
        rules.add(rule);
        Vector<Object> row = new Vector<>();
        row.add(rule.getSourceType().toString());
        row.add(rule.getSourceValue());
        row.add(rule.getTargetColumn());
        row.add("Calculating..."); // Placeholder for count
        tableModel.addRow(row);
        return tableModel.getRowCount() - 1; // Return the index of the new row
    }

    public void updateRuleCount(int rowIndex, int count) {
        tableModel.setValueAt(count, rowIndex, 3);
    }

    public void clearRules() {
        rules.clear();
        tableModel.setRowCount(0);
    }

    public List<FilterRule> getRules() {
        return new ArrayList<>(rules);
    }

    private static class RecordCountRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof Integer) {
                int count = (Integer) value;
                if (count == 0) {
                    c.setForeground(Color.RED);
                } else {
                    c.setForeground(new Color(0, 128, 0)); // Dark Green
                }
            } else {
                c.setForeground(Color.BLUE); // "Calculating..."
            }
            return c;
        }
    }
}
