package com.excelutility.gui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColumnMappingPanel extends JPanel {

    private final ColumnMappingTableModel tableModel;
    private final JTable mappingTable;

    public ColumnMappingPanel() {
        setLayout(new MigLayout("fill, insets 5", "[grow]", "[grow][]"));
        setBorder(BorderFactory.createTitledBorder("Column Mappings & Row Matching"));

        tableModel = new ColumnMappingTableModel();
        mappingTable = new JTable(tableModel);
        mappingTable.setRowHeight(25);
        mappingTable.getTableHeader().setReorderingAllowed(false);
        add(new JScrollPane(mappingTable), "grow, wrap, hmin 150");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton autoMapButton = new JButton("Auto-map");
        // autoMapButton.addActionListener(e -> autoMap()); // TODO
        buttonPanel.add(autoMapButton);

        add(buttonPanel, "growx");
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
            boolean ignored = (boolean) rowData[2];
            if (!ignored && rowData[1] != null && !rowData[1].toString().isEmpty()) {
                mappings.put(rowData[0].toString(), rowData[1].toString());
            }
        }
        return mappings;
    }
}
