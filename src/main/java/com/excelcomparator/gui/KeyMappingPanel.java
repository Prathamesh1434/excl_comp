package com.excelcomparator.gui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A panel that manages a dynamic list of KeyMappingRow panels.
 */
import java.util.Map;
import java.util.HashMap;

public class KeyMappingPanel extends JPanel {
    private final JPanel rowsPanel;
    private final List<KeyMappingRow> mappingRows = new ArrayList<>();
    private List<String> file1Columns = new ArrayList<>();
    private List<String> file2Columns = new ArrayList<>();
    private JCheckBox columnLevelCheckBox;
    private JCheckBox rowPresenceCheckBox;

    public KeyMappingPanel() {
        setLayout(new BorderLayout());

        JPanel optionsCheckBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        columnLevelCheckBox = new JCheckBox("Column-level comparison", true);
        rowPresenceCheckBox = new JCheckBox("Detect missing/extra rows", true);
        optionsCheckBoxPanel.add(columnLevelCheckBox);
        optionsCheckBoxPanel.add(rowPresenceCheckBox);
        add(optionsCheckBoxPanel, BorderLayout.NORTH);

        rowsPanel = new JPanel();
        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
        add(new JScrollPane(rowsPanel), BorderLayout.CENTER);

        JButton addButton = new JButton("+ Add Key");
        addButton.addActionListener(e -> addMappingRow());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(addButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // Start with one row
        addMappingRow();
    }

    private void addMappingRow() {
        KeyMappingRow newRow = new KeyMappingRow();
        newRow.setRemoveButtonAction(e -> removeMappingRow(newRow));
        mappingRows.add(newRow);
        rowsPanel.add(newRow);
        updateColumnModels(newRow);
        revalidate();
        repaint();
    }

    private void removeMappingRow(KeyMappingRow row) {
        if (mappingRows.size() > 1) { // Always keep at least one row
            mappingRows.remove(row);
            rowsPanel.remove(row);
            revalidate();
            repaint();
        }
    }

    public void setFile1Columns(List<String> columns) {
        this.file1Columns = new ArrayList<>(columns);
        for (KeyMappingRow row : mappingRows) {
            row.getFile1ColumnComboBox().setModel(new DefaultComboBoxModel<>(columns.toArray(new String[0])));
        }
    }

    public void setFile2Columns(List<String> columns) {
        this.file2Columns = new ArrayList<>(columns);
        for (KeyMappingRow row : mappingRows) {
            row.getFile2ColumnComboBox().setModel(new DefaultComboBoxModel<>(columns.toArray(new String[0])));
        }
    }

    private void updateColumnModels(KeyMappingRow row) {
        row.getFile1ColumnComboBox().setModel(new DefaultComboBoxModel<>(file1Columns.toArray(new String[0])));
        row.getFile2ColumnComboBox().setModel(new DefaultComboBoxModel<>(file2Columns.toArray(new String[0])));
    }

    /**
     * Gets the selected key mappings from the UI.
     *
     * @return A map where the key is the column index from file 1 and the value is the column index from file 2.
     */
    public Map<Integer, Integer> getKeyMappings() {
        Map<Integer, Integer> mappings = new HashMap<>();
        for (KeyMappingRow row : mappingRows) {
            int index1 = row.getFile1ColumnComboBox().getSelectedIndex();
            int index2 = row.getFile2ColumnComboBox().getSelectedIndex();
            if (index1 != -1 && index2 != -1) {
                mappings.put(index1, index2);
            }
        }
        return mappings;
    }

    public boolean isColumnLevelComparisonEnabled() {
        return columnLevelCheckBox.isSelected();
    }

    public boolean isRowPresenceCheckEnabled() {
        return rowPresenceCheckBox.isSelected();
    }

    public void clear() {
        // Remove all but the first row
        while (mappingRows.size() > 1) {
            removeMappingRow(mappingRows.get(mappingRows.size() - 1));
        }
        // Clear the models in the first row
        if (!mappingRows.isEmpty()) {
            mappingRows.get(0).getFile1ColumnComboBox().setModel(new DefaultComboBoxModel<>());
            mappingRows.get(0).getFile2ColumnComboBox().setModel(new DefaultComboBoxModel<>());
        }
    }
}
