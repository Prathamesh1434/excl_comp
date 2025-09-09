package com.excelutility.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Vector;

/**
 * A panel that displays a preview of Excel data in a JTable.
 */
public class PreviewPanel extends JPanel {

    private JTable table;
    private DefaultTableModel tableModel;

    /**
     * Constructs a PreviewPanel with a title.
     *
     * @param title The title of the panel.
     */
    public PreviewPanel(String title) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(title));

        tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Updates the table with new data.
     *
     * @param data    The data to display in the table.
     * @param headers The column headers for the table.
     */
    public void updateData(List<List<Object>> data, List<Object> headers) {
        Vector<String> headerVector = new Vector<>();
        for (Object header : headers) {
            headerVector.add(header != null ? header.toString() : "");
        }

        Vector<Vector<Object>> dataVector = new Vector<>();
        for (List<Object> row : data) {
            dataVector.add(new Vector<>(row));
        }

        tableModel.setDataVector(dataVector, headerVector);
    }
}
