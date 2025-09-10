package com.excelutility.gui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import net.miginfocom.swing.MigLayout;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColumnMappingPanel extends JPanel {

    private final ColumnMappingTableModel tableModel;
    private final JTable mappingTable;
    private final JList<String> keyList;
    private final DefaultListModel<String> keyListModel;

    public ColumnMappingPanel() {
        setLayout(new MigLayout("fill, insets 5", "[grow, 70%][grow, 30%]", "[grow][]"));
        setBorder(BorderFactory.createTitledBorder("Column Mappings & Row Matching"));

        tableModel = new ColumnMappingTableModel();
        mappingTable = new JTable(tableModel);
        mappingTable.setRowHeight(25);
        mappingTable.getTableHeader().setReorderingAllowed(false);

        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                updateKeyList();
            }
        });

        add(new JScrollPane(mappingTable), "grow, hmin 150");

        // Key List Panel
        keyListModel = new DefaultListModel<>();
        keyList = new JList<>(keyListModel);
        JPanel keyPanel = new JPanel(new MigLayout("fill", "[grow]", "[grow]"));
        keyPanel.setBorder(BorderFactory.createTitledBorder("Selected Keys"));
        keyPanel.add(new JScrollPane(keyList), "grow");
        add(keyPanel, "grow, wrap");

        // Bottom button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton autoMapButton = new JButton("Auto-map");
        buttonPanel.add(autoMapButton);

        add(buttonPanel, "growx, span 2");
    }

    public void setColumns(List<String> sourceCols, List<String> targetCols) {
        tableModel.setSourceColumns(sourceCols, targetCols);

        // Set up the JComboBox editor for the target column
        TableColumn targetColumn = mappingTable.getColumnModel().getColumn(1);
        JComboBox<String> comboBox = new JComboBox<>();
        if (targetCols != null) {
            targetCols.forEach(comboBox::addItem);
        }
        targetColumn.setCellEditor(new DefaultCellEditor(comboBox));
    }

    public Map<String, String> getColumnMappings() {
        Map<String, String> mappings = new HashMap<>();
        for (Object[] rowData : tableModel.getMappingData()) {
            boolean ignored = (boolean) rowData[3]; // Index 3 is "Ignore"
            if (!ignored && rowData[1] != null && !rowData[1].toString().isEmpty()) {
                mappings.put(rowData[0].toString(), rowData[1].toString());
            }
        }
        return mappings;
    }

    public List<String> getKeyColumns() {
        List<String> keyColumns = new ArrayList<>();
        for (Object[] rowData : tableModel.getMappingData()) {
            boolean isKey = (boolean) rowData[2]; // Index 2 is "Is Key"
            if (isKey) {
                keyColumns.add(rowData[0].toString());
            }
        }
        return keyColumns;
    }

    private void updateKeyList() {
        keyListModel.clear();
        for (String key : getKeyColumns()) {
            keyListModel.addElement(key);
        }
    }

    public void selectKeys(List<String> keysToSelect) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String sourceColumn = tableModel.getValueAt(i, 0).toString();
            boolean shouldBeSelected = keysToSelect.contains(sourceColumn);
            tableModel.setValueAt(shouldBeSelected, i, 2); // Column 2 is "Is Key"
        }
    }
}
