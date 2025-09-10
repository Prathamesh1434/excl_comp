package com.excelutility.gui;

import com.excelutility.core.CellDifference;
import com.excelutility.core.RowResult;
import com.excelutility.core.RowComparisonStatus;

import com.excelutility.core.CellDifference;
import com.excelutility.core.RowResult;
import com.excelutility.core.RowComparisonStatus;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Map;

/**
 * A custom table cell renderer for displaying comparison results with color-coding.
 */
public class ResultTableRenderer extends DefaultTableCellRenderer {

    private static final Color COLOR_MISMATCH_ROW = new Color(255, 255, 204); // Light Yellow
    private static final Color COLOR_MISMATCH_CELL = new Color(255, 200, 100); // Orange
    private static final Color COLOR_SOURCE_ONLY = new Color(204, 255, 204); // Light Green
    private static final Color COLOR_TARGET_ONLY = new Color(255, 204, 204); // Light Pink

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (!(table.getModel() instanceof ResultTableModel)) {
            return c;
        }

        ResultTableModel model = (ResultTableModel) table.getModel();
        RowResult rowResult = model.getRowResult(row);
        RowComparisonStatus status = rowResult.getStatus();

        // Reset tooltip
        setToolTipText(null);

        // Set colors based on status
        Color defaultBgColor = table.getBackground();
        Color rowColor = defaultBgColor;

        if (status != null) {
            switch (status) {
                case MATCHED_MISMATCHED:
                    rowColor = COLOR_MISMATCH_ROW;
                    Map<Integer, CellDifference> diffs = rowResult.getDifferences();
                    if (diffs != null && diffs.containsKey(column)) {
                        c.setBackground(COLOR_MISMATCH_CELL);
                        CellDifference diff = diffs.get(column);
                        setToolTipText("<html>Source: " + diff.getSourceValue() + "<br>Target: " + diff.getTargetValue() + "</html>");
                    } else {
                        c.setBackground(rowColor);
                    }
                    break;
                case SOURCE_ONLY:
                    rowColor = COLOR_SOURCE_ONLY;
                    c.setBackground(rowColor);
                    break;
                case TARGET_ONLY:
                    rowColor = COLOR_TARGET_ONLY;
                    c.setBackground(rowColor);
                    break;
                case MATCHED_IDENTICAL:
                default:
                    c.setBackground(defaultBgColor);
                    break;
            }
        }

        if (isSelected) {
            c.setBackground(table.getSelectionBackground());
        }

        return c;
    }
}
