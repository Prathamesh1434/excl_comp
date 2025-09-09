package com.excelcomparator.gui;

import com.excelcomparator.compare.CellDifference;
import com.excelcomparator.compare.RowResult;
import com.excelcomparator.compare.RowComparisonStatus;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * A custom table cell renderer for displaying comparison results with color-coding.
 */
public class ResultTableRenderer extends DefaultTableCellRenderer {

    private static final Color MISMATCH_COLOR = new Color(255, 255, 153); // Light Yellow
    private static final Color MISSING_COLOR = new Color(255, 182, 193); // Light Pink
    private static final Color MISMATCH_CELL_COLOR = new Color(255, 204, 153); // Light Orange

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (!(table.getModel() instanceof ResultTableModel)) {
            return c;
        }

        ResultTableModel model = (ResultTableModel) table.getModel();
        RowResult rowResult = model.getRowResult(row);
        RowComparisonStatus status = rowResult.getStatus();

        setToolTipText(null); // Default tooltip

        if (status != null) {
            switch (status) {
                case IDENTICAL:
                    c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                    break;
                case MISSING_IN_FILE1:
                case MISSING_IN_FILE2:
                    c.setBackground(isSelected ? table.getSelectionBackground() : MISSING_COLOR);
                    break;
                case MISMATCHED:
                    c.setBackground(isSelected ? table.getSelectionBackground() : MISMATCH_COLOR);
                    if (rowResult.getDifferences().containsKey(column)) {
                        c.setBackground(isSelected ? table.getSelectionBackground() : MISMATCH_CELL_COLOR);
                        CellDifference diff = rowResult.getDifferences().get(column);
                        setToolTipText("File1: " + diff.getValue1() + " vs File2: " + diff.getValue2());
                    }
                    break;
            }
        }

        if (isSelected) {
            c.setForeground(table.getSelectionForeground());
        } else {
            c.setForeground(table.getForeground());
        }

        return c;
    }
}
