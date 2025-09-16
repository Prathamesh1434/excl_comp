package com.excelutility.gui;

import com.excelutility.io.SimpleExcelWriter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A panel that holds a results table and action buttons (Copy, Download) for that table.
 */
public class ResultTabPanel extends JPanel {

    private final JTable resultsTable;

    public ResultTabPanel(JTable table) {
        this.resultsTable = table;
        setLayout(new BorderLayout(5, 5));

        // Table View
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        add(scrollPane, BorderLayout.CENTER);

        // Action Bar
        JPanel actionBar = new JPanel(new MigLayout("insets 0, align right"));
        JButton copyButton = new JButton("Copy to Clipboard");
        JButton downloadButton = new JButton("Download as Excel");

        actionBar.add(copyButton);
        actionBar.add(downloadButton);
        add(actionBar, BorderLayout.SOUTH);

        // Action Listeners
        copyButton.addActionListener(e -> copyTableToClipboard());
        downloadButton.addActionListener(e -> downloadTable());
    }

    private void copyTableToClipboard() {
        TableModel model = resultsTable.getModel();
        int numCols = model.getColumnCount();
        int numRows = model.getRowCount();

        StringBuilder sb = new StringBuilder();

        // Append header
        for (int i = 0; i < numCols; i++) {
            sb.append(model.getColumnName(i));
            if (i < numCols - 1) {
                sb.append("\t");
            }
        }
        sb.append("\n");

        // Append rows
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                Object value = model.getValueAt(i, j);
                sb.append(value == null ? "" : value.toString());
                if (j < numCols - 1) {
                    sb.append("\t");
                }
            }
            sb.append("\n");
        }

        StringSelection selection = new StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        JOptionPane.showMessageDialog(this, "Table data copied to clipboard.", "Copy Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    private void downloadTable() {
        TableModel model = resultsTable.getModel();
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "There is no data to download.", "Empty Table", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Results");
        chooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = chooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".xlsx")) {
                filePath += ".xlsx";
            }

            try {
                List<List<Object>> data = new ArrayList<>();
                // Add header
                List<Object> header = IntStream.range(0, model.getColumnCount())
                        .mapToObj(model::getColumnName)
                        .collect(Collectors.toList());
                data.add(header);

                // Add rows
                for (int i = 0; i < model.getRowCount(); i++) {
                    List<Object> row = new ArrayList<>();
                    for (int j = 0; j < model.getColumnCount(); j++) {
                        row.add(model.getValueAt(i, j));
                    }
                    data.add(row);
                }

                Map<String, List<List<Object>>> results = Map.of("Results", data);
                SimpleExcelWriter.writeFilteredResults(filePath, results, false, Color.WHITE); // No highlighting for these exports
                JOptionPane.showMessageDialog(this, "Results exported successfully!", "Export Complete", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to export results: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
