package com.excelutility.gui;

import com.excelutility.core.ComparisonResult;
import com.excelutility.core.RowResult;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class ResultTableModel extends AbstractTableModel {

    private List<String> columnNames = new ArrayList<>();
    private List<RowResult> results = new ArrayList<>();

    public void setComparisonResult(ComparisonResult result) {
        this.columnNames = result.getFinalHeaders();
        this.results = result.getRowResults();
        fireTableStructureChanged();
    }

    public void clear() {
        this.columnNames.clear();
        this.results.clear();
        fireTableStructureChanged();
    }

    @Override
    public int getRowCount() {
        return results.size();
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
        RowResult rowResult = results.get(rowIndex);
        // For simplicity, just show source data for now
        if (rowResult.getSourceRowData() != null && columnIndex < rowResult.getSourceRowData().size()) {
            return rowResult.getSourceRowData().get(columnIndex);
        }
        if (rowResult.getTargetRowData() != null && columnIndex < rowResult.getTargetRowData().size()) {
            return rowResult.getTargetRowData().get(columnIndex);
        }
        return "";
    }

    public RowResult getRowResult(int rowIndex) {
        return results.get(rowIndex);
    }
}
