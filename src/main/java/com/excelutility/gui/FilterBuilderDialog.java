package com.excelutility.gui;

import com.excelutility.core.FilterGroup;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class FilterBuilderDialog extends JDialog {

    private final List<String> availableColumns;
    private final JPanel conditionsPanel;
    private final FilterGroup filterGroup = new FilterGroup(); // The root group

    public FilterBuilderDialog(Frame owner, List<String> availableColumns) {
        super(owner, "Filter Builder", true);
        this.availableColumns = availableColumns;
        setSize(800, 400);
        setLocationRelativeTo(owner);

        setLayout(new BorderLayout(10, 10));

        conditionsPanel = new JPanel();
        conditionsPanel.setLayout(new BoxLayout(conditionsPanel, BoxLayout.Y_AXIS));
        add(new JScrollPane(conditionsPanel), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addConditionButton = new JButton("+ Add Condition");
        addConditionButton.addActionListener(e -> addConditionRow());
        buttonPanel.add(addConditionButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Add an initial condition row
        addConditionRow();
    }

    private void addConditionRow() {
        FilterConditionPanel newRow = new FilterConditionPanel(availableColumns);
        newRow.getRemoveButton().addActionListener(e -> {
            conditionsPanel.remove(newRow);
            revalidate();
            repaint();
        });
        conditionsPanel.add(newRow);
        revalidate();
        repaint();
    }

    public FilterGroup getFilterGroup() {
        // TODO: Build the FilterGroup from the UI components
        return filterGroup;
    }
}
