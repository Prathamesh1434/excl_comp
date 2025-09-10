package com.excelutility.gui;

import com.excelutility.core.FilterRule;
import com.excelutility.core.FilteringService;
import com.excelutility.io.ExcelReader;
import com.excelutility.io.SimpleExcelWriter;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class FilterFrame extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(FilterFrame.class);

    private final FileSelectionPanel dataFilePanel;
    private final FileSelectionPanel filterValuesFilePanel;
    private final JTable dataPreviewTable;
    private final JTable filterValuesPreviewTable;
    private final DefaultTableModel dataPreviewModel;
    private final DefaultTableModel filterValuesPreviewModel;
    private final FilterRulesPanel filterRulesPanel;
    private final FilteringService filteringService = new FilteringService();

    private final JCheckBox mergeOption;
    private Color selectedColor = Color.YELLOW;

    public FilterFrame() {
        setTitle("Excel Filter");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1600, 1000);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- Top Panel for File Selection ---
        JPanel topPanel = new JPanel(new MigLayout("fillx", "[grow][grow]"));
        dataFilePanel = new FileSelectionPanel("Data File (to be filtered)", this);
        filterValuesFilePanel = new FileSelectionPanel("Filter Values File", this);
        topPanel.add(dataFilePanel, "growx");
        topPanel.add(filterValuesFilePanel, "growx, wrap");

        JButton previewButton = new JButton("Load & Preview Files");
        topPanel.add(previewButton, "span, center");
        add(topPanel, BorderLayout.NORTH);

        // --- Center Panel for Previews and Rules ---
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplit.setResizeWeight(0.7);

        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setResizeWeight(0.5);

        // Data Preview Table
        dataPreviewModel = new DefaultTableModel();
        dataPreviewTable = new JTable(dataPreviewModel);
        JScrollPane dataPreviewScroll = new JScrollPane(dataPreviewTable);
        dataPreviewScroll.setBorder(BorderFactory.createTitledBorder("Data Preview (First 50 rows)"));
        centerSplit.setLeftComponent(dataPreviewScroll);

        // Filter Values Preview Table
        filterValuesPreviewModel = new DefaultTableModel();
        filterValuesPreviewTable = new JTable(filterValuesPreviewModel);
        JScrollPane filterValuesPreviewScroll = new JScrollPane(filterValuesPreviewTable);
        filterValuesPreviewScroll.setBorder(BorderFactory.createTitledBorder("Filter Values Preview (Full Data) - Double-click a cell to create a filter"));
        centerSplit.setRightComponent(filterValuesPreviewScroll);

        mainSplit.setTopComponent(centerSplit);

        // --- Bottom Panel for Rules and Actions ---
        JPanel bottomPanel = new JPanel(new MigLayout("fill", "[grow][nogrid]", "[grow]"));
        filterRulesPanel = new FilterRulesPanel();
        bottomPanel.add(filterRulesPanel, "grow");

        JPanel actionPanel = new JPanel(new MigLayout("wrap 1", "[grow]"));
        JButton addFilterButton = new JButton("Add Filter from Selection");
        mergeOption = new JCheckBox("Merge all results into one file", true);
        JButton colorButton = new JButton("Set Highlight Color");
        JButton downloadButton = new JButton("Download Filtered Results");

        actionPanel.add(addFilterButton, "growx");
        actionPanel.add(mergeOption, "growx");
        actionPanel.add(colorButton, "growx");
        actionPanel.add(downloadButton, "growx, gaptop 20");
        bottomPanel.add(actionPanel);

        mainSplit.setBottomComponent(bottomPanel);
        add(mainSplit, BorderLayout.CENTER);

        // Action Listeners
        previewButton.addActionListener(e -> loadPreviews());
        addFilterButton.addActionListener(e -> createFilterFromSelection());
        colorButton.addActionListener(e -> chooseColor());
        downloadButton.addActionListener(e -> downloadResults());

        filterValuesPreviewTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // Double-click
                    createFilterFromSelection();
                }
            }
        });
    }

    private void chooseColor() {
        Color newColor = JColorChooser.showDialog(this, "Choose Highlight Color", selectedColor);
        if (newColor != null) {
            selectedColor = newColor;
        }
    }

    private void downloadResults() {
        List<FilterRule> rules = filterRulesPanel.getRules();
        if (rules.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add at least one filter rule.", "No Filters", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Filtered Results");
        chooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = chooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".xlsx")) {
                filePath += ".xlsx";
            }

            final String finalFilePath = filePath;
            final boolean merge = mergeOption.isSelected();
            logger.info("Starting export. Merge: {}, Color: {}", merge, selectedColor);

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    Map<String, List<List<Object>>> filteredData = filteringService.filter(
                            dataFilePanel.getFilePath(),
                            dataFilePanel.getSelectedSheet(),
                            rules,
                            filterValuesFilePanel.getFilePath(),
                            filterValuesFilePanel.getSelectedSheet()
                    );

                    if (filteredData.isEmpty()) {
                        throw new Exception("No data matched the specified filters.");
                    }

                    SimpleExcelWriter.writeFilteredResults(finalFilePath, filteredData, merge, selectedColor);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        logger.info("Export completed successfully.");
                        JOptionPane.showMessageDialog(FilterFrame.this, "Results exported successfully!", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        logger.error("Export failed.", e);
                        JOptionPane.showMessageDialog(FilterFrame.this, "Failed to export results: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void createFilterFromSelection() {
        int selectedRow = filterValuesPreviewTable.getSelectedRow();
        int selectedCol = filterValuesPreviewTable.getSelectedColumn();

        if (selectedRow == -1 || selectedCol == -1) {
            JOptionPane.showMessageDialog(this, "Please select a cell in the 'Filter Values' table first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String cellValue = filterValuesPreviewTable.getValueAt(selectedRow, selectedCol).toString();
        String columnName = filterValuesPreviewTable.getColumnName(selectedCol);

        FilterSourceDialog sourceDialog = new FilterSourceDialog(this, cellValue, columnName);
        sourceDialog.setVisible(true);

        FilterRule.SourceType sourceType = sourceDialog.getSelectedType();
        String sourceValue = sourceDialog.getSelectedValue();

        if (sourceType != null) {
            List<String> targetColumns = dataFilePanel.getColumnNames();
            if (targetColumns.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Could not retrieve column names from the data file. Please ensure it is loaded correctly.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            FilterTargetDialog targetDialog = new FilterTargetDialog(this, targetColumns);
            targetDialog.setVisible(true);

            List<String> selectedTargets = targetDialog.getSelectedColumns();
            for (String target : selectedTargets) {
                filterRulesPanel.addRule(new FilterRule(sourceType, sourceValue, target));
            }
        }
    }

    private void loadPreviews() {
        loadTableData(dataFilePanel, dataPreviewModel, 50, "Error loading data preview");
        loadTableData(filterValuesFilePanel, filterValuesPreviewModel, -1, "Error loading filter values preview");
    }

    private void loadTableData(FileSelectionPanel panel, DefaultTableModel model, int rowLimit, String errorTitle) {
        String filePath = panel.getFilePath();
        String sheetName = panel.getSelectedSheet();

        if (filePath == null || filePath.trim().isEmpty() || sheetName == null) {
            JOptionPane.showMessageDialog(this, "Please select a file and a sheet.", "File Not Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new SwingWorker<List<List<Object>>, Void>() {
            @Override
            protected List<List<Object>> doInBackground() throws Exception {
                if (rowLimit > 0) {
                    return ExcelReader.readPreview(filePath, sheetName, rowLimit);
                } else {
                    return ExcelReader.read(filePath, sheetName, true);
                }
            }

            @Override
            protected void done() {
                try {
                    List<List<Object>> data = get();
                    if (data == null || data.isEmpty()) {
                        model.setDataVector(new Vector<>(), new Vector<>());
                        return;
                    }

                    Vector<String> headers = new Vector<>();
                    for (Object header : data.get(0)) {
                        headers.add(header != null ? header.toString() : "");
                    }

                    Vector<Vector<Object>> dataVector = new Vector<>();
                    if (data.size() > 1) {
                        for (int i = 1; i < data.size(); i++) {
                            dataVector.add(new Vector<>(data.get(i)));
                        }
                    }
                    model.setDataVector(dataVector, headers);

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(FilterFrame.this, "Could not load data: " + e.getMessage(), errorTitle, JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
