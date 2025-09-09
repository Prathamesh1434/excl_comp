package com.excelcomparator.gui;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple table model to display a preview of Excel data.
 */
public class PreviewTableModel extends AbstractTableModel {
    private List<String> columnNames = new ArrayList<>();
    private List<List<Object>> data = new ArrayList<>();

    /**
     * Sets the data for the table model.
     * The first row of the data is assumed to be the header.
     *
     * @param data The data to display, including a header row.
     */
    public void setData(List<List<Object>> data) {
        this.columnNames.clear();
        this.data.clear();

        if (data != null && !data.isEmpty()) {
            List<Object> headerRow = data.get(0);
            for (Object header : headerRow) {
                this.columnNames.add(header != null ? header.toString() : "");
            }
            if (data.size() > 1) {
                this.data.addAll(data.subList(1, data.size()));
            }
        }
        fireTableStructureChanged();
    }

    /**
     * Clears all data from the table model.
     */
    public void clearData() {
        this.columnNames.clear();
        this.data.clear();
        fireTableStructureChanged();
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public String getColumnName(int column) {
        return columnNames.get(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex < data.get(rowIndex).size()) {
            return data.get(rowIndex).get(columnIndex);
        }
        return "";
    }
}
