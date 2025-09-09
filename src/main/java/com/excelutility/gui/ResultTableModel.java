package com.excelutility.gui;

import com.excelutility.compare.ComparisonReport;
import com.excelutility.compare.RowComparisonResult;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A custom table model to display comparison results.
 */
public class ResultTableModel extends AbstractTableModel {
    private ComparisonReport report;
    private List<String> columnNames;

    public ResultTableModel() {
        this.report = new ComparisonReport(new ArrayList<>(), new ArrayList<>());
        this.columnNames = new ArrayList<>();
    }

    /**
     * Sets the comparison report and updates the table structure.
     *
     * @param report The comparison report to display.
     */
    public void setReport(ComparisonReport report) {
        this.report = report;
        this.columnNames = new ArrayList<>();
        if (report != null && report.getHeaders() != null) {
            for (Object header : report.getHeaders()) {
                this.columnNames.add(header != null ? header.toString() : "");
            }
        }
        fireTableStructureChanged();
    }

    @Override
    public int getRowCount() {
        return report != null ? report.getResults().size() : 0;
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
        if (report == null) return null;
        RowComparisonResult rowResult = report.getResults().get(rowIndex);
        if (columnIndex < rowResult.getRowData().size()) {
            return rowResult.getRowData().get(columnIndex);
        }
        return "";
    }

    /**
     * Gets the RowComparisonResult for a specific row index.
     *
     * @param rowIndex The index of the row.
     * @return The RowComparisonResult for the given row.
     */
    public RowComparisonResult getRowResult(int rowIndex) {
        if (report == null || rowIndex < 0 || rowIndex >= report.getResults().size()) {
            return new RowComparisonResult(null, Collections.emptyList(), Collections.emptyMap());
        }
        return report.getResults().get(rowIndex);
    }
}
