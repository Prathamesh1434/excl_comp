package com.excelutility.gui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FilterTargetDialog extends JDialog {

    private JList<String> columnList;
    private List<String> selectedColumns = Collections.emptyList();
    private JCheckBox trimWhitespaceCheckbox;
    private final List<String> allColumns;

    public FilterTargetDialog(Frame owner, List<String> availableColumns) {
        super(owner, "Select Target Column(s) for Filter", true);
        this.allColumns = availableColumns;
        setLayout(new MigLayout("fill, wrap 1", "[grow]", "[][][grow][]"));

        add(new JLabel("Select one or more columns from the data file to apply the filter to:"), "growx");

        JTextField searchField = new JTextField();
        add(new JLabel("Search:"));
        add(searchField, "growx");

        columnList = new JList<>(allColumns.toArray(new String[0]));
        columnList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        add(new JScrollPane(columnList), "grow");

        trimWhitespaceCheckbox = new JCheckBox("Trim whitespace from target column(s) before filtering", true);
        add(trimWhitespaceCheckbox, "growx");

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterList(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterList(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterList(); }

            private void filterList() {
                String searchTerm = searchField.getText().toLowerCase();
                DefaultListModel<String> model = new DefaultListModel<>();
                for (String col : allColumns) {
                    if (col.toLowerCase().contains(searchTerm)) {
                        model.addElement(col);
                    }
                }
                columnList.setModel(model);
            }
        });

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
