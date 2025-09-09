package com.excelutility.gui;

import com.excelutility.compare.CellComparisonResult;
import com.excelutility.compare.RowComparisonResult;
import com.excelutility.compare.RowComparisonStatus;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * A custom table cell renderer to color-code comparison results.
 */
public class ResultTableRenderer extends DefaultTableCellRenderer {

    private static final Color MISMATCH_COLOR = new Color(255, 255, 153); // Light Yellow
    private static final Color MISSING_COLOR = new Color(255, 182, 193); // Light Pink
    private static final Color MISMATCH_CELL_COLOR = new Color(255, 204, 153); // Light Orange

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (table.getModel() instanceof ResultTableModel) {
            ResultTableModel model = (ResultTableModel) table.getModel();
            RowComparisonResult rowResult = model.getRowResult(row);
            RowComparisonStatus status = rowResult.getStatus();

            // Set default tooltip to null
            setToolTipText(null);

            if (status != null) {
                switch (status) {
                    case IDENTICAL:
                        c.setBackground(table.getBackground());
                        break;
                    case MISSING_IN_FILE1:
                    case MISSING_IN_FILE2:
                        c.setBackground(MISSING_COLOR);
                        break;
                    case MISMATCHED:
                        c.setBackground(MISMATCH_COLOR);
                        if (rowResult.getMismatchedCells().containsKey(column)) {
                            c.setBackground(MISMATCH_CELL_COLOR);
                            CellComparisonResult cellResult = rowResult.getMismatchedCells().get(column);
                            setToolTipText("File1: " + cellResult.getValue1() + " vs File2: " + cellResult.getValue2());
                        }
                        break;
                }
            }

            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
                c.setForeground(table.getSelectionForeground());
            } else {
                 c.setForeground(table.getForeground());
            }
        }

        return c;
    }
}
