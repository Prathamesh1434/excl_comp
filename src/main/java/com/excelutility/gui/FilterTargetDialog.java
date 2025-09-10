package com.excelutility.gui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class FilterTargetDialog extends JDialog {

    private JList<String> columnList;
    private List<String> selectedColumns = Collections.emptyList();
    private JCheckBox trimWhitespaceCheckbox;

    public FilterTargetDialog(Frame owner, List<String> availableColumns) {
        super(owner, "Select Target Column(s) for Filter", true);
        setLayout(new MigLayout("fill, wrap 1", "[grow]", "[][grow][]"));

        add(new JLabel("Select one or more columns from the data file to apply the filter to:"), "growx");

        columnList = new JList<>(availableColumns.toArray(new String[0]));
        columnList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        add(new JScrollPane(columnList), "grow");

        trimWhitespaceCheckbox = new JCheckBox("Trim whitespace from target column(s) before filtering", true);
        add(trimWhitespaceCheckbox, "growx");

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            selectedColumns = columnList.getSelectedValuesList();
            if (selectedColumns.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select at least one target column.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            setVisible(false);
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            selectedColumns = Collections.emptyList();
            setVisible(false);
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, "growx, right");

        setSize(400, 500);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    public List<String> getSelectedColumns() {
        return selectedColumns;
    }

    public boolean isTrimWhitespaceSelected() {
        return trimWhitespaceCheckbox.isSelected();
    }
}
