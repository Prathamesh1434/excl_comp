package com.excelutility.gui;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Table model for the column mapping grid.
 */
public class ColumnMappingTableModel extends AbstractTableModel {

    private final String[] columnNames = {"Source Column", "Target Column", "Ignore"};
    private List<Object[]> data = new ArrayList<>(); // Data: [String source, String target, Boolean ignore]

    public void setSourceColumns(List<String> sourceColumns, List<String> targetColumns) {
        data.clear();
        for (String sourceCol : sourceColumns) {
            // Basic auto-mapping
            String targetCol = targetColumns.stream()
                    .filter(t -> t.equalsIgnoreCase(sourceCol))
                    .findFirst()
                    .orElse(null);
            data.add(new Object[]{sourceCol, targetCol, false});
        }
        fireTableDataChanged();
    }

    public List<Object[]> getMappingData() {
        return data;
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 2) {
            return Boolean.class;
        }
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // Source column is not editable
        return columnIndex > 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return data.get(rowIndex)[columnIndex];
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        data.get(rowIndex)[columnIndex] = aValue;
        fireTableCellUpdated(rowIndex, columnIndex);
    }
}
