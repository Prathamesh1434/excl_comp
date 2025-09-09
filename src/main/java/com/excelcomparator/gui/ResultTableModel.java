package com.excelcomparator.gui;

import com.excelcomparator.compare.ComparisonResult;
import com.excelcomparator.compare.RowResult;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * A custom table model to display the comparison results.
 */
public class ResultTableModel extends AbstractTableModel {

    private List<String> columnNames = new ArrayList<>();
    private List<RowResult> rowResults = new ArrayList<>();

    public void setComparisonResult(ComparisonResult result) {
        this.columnNames.clear();
        this.rowResults.clear();
        if (result != null) {
            for(Object header : result.getHeaders()){
                columnNames.add(header.toString());
            }
            this.rowResults.addAll(result.getRowResults());
        }
        fireTableStructureChanged();
    }

    public void clear() {
        this.columnNames.clear();
        this.rowResults.clear();
        fireTableStructureChanged();
    }

    public RowResult getRowResult(int rowIndex) {
        return rowResults.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return rowResults.size();
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
        List<Object> rowData = rowResults.get(rowIndex).getRowData();
        if (columnIndex < rowData.size()) {
            return rowData.get(columnIndex);
        }
        return "";
    }
}
