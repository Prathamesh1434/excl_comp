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
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class FilterPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(FilterPanel.class);

    private final FilterFilePanel dataFilePanel;
    private final FilterFilePanel filterValuesFilePanel;
    private final JTable dataPreviewTable;
    private final JTable filterValuesPreviewTable;
    private final DefaultTableModel dataPreviewModel;
    private final DefaultTableModel filterValuesPreviewModel;
    private final FilterRulesPanel filterRulesPanel;
    private final FilteringService filteringService = new FilteringService();

    private final JCheckBox mergeOption;
    private Color selectedColor = Color.YELLOW;

    public FilterPanel() {
        setLayout(new BorderLayout());

        // --- Top Panel for File Selection ---
        JPanel topPanel = new JPanel(new MigLayout("fillx", "[grow][grow]"));
        dataFilePanel = new FilterFilePanel("Data File (to be filtered)", this);
        filterValuesFilePanel = new FilterFilePanel("Filter Values File", this);
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
        configureTable(dataPreviewTable);
        JScrollPane dataPreviewScroll = new JScrollPane(dataPreviewTable);
        dataPreviewScroll.setBorder(BorderFactory.createTitledBorder("Data Preview (First 50 rows)"));
        centerSplit.setLeftComponent(dataPreviewScroll);

        // Filter Values Preview Table
        filterValuesPreviewModel = new DefaultTableModel();
        filterValuesPreviewTable = new JTable(filterValuesPreviewModel);
        configureTable(filterValuesPreviewTable);
        filterValuesPreviewTable.setCellSelectionEnabled(true);
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

        JTextField searchField = new JTextField();
        JButton searchButton = new JButton("Search Previews");

        actionPanel.add(new JLabel("Preview Search:"), "split 2");
        actionPanel.add(searchField, "growx");
        actionPanel.add(searchButton, "wrap, gaptop 5");

        actionPanel.add(addFilterButton, "growx, gaptop 10");
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
        searchButton.addActionListener(e -> searchTables(searchField.getText()));

        filterValuesPreviewTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // Double-click
                    createFilterFromSelection();
                }
            }
        });
    }

    private void searchTables(String text) {
        if (text == null || text.trim().isEmpty()) {
            ((TableRowSorter) dataPreviewTable.getRowSorter()).setRowFilter(null);
            ((TableRowSorter) filterValuesPreviewTable.getRowSorter()).setRowFilter(null);
        } else {
            RowFilter<Object, Object> rf = RowFilter.regexFilter("(?i)" + Pattern.quote(text));
            ((TableRowSorter) dataPreviewTable.getRowSorter()).setRowFilter(rf);
            ((TableRowSorter) filterValuesPreviewTable.getRowSorter()).setRowFilter(rf);
        }
    }

    private void configureTable(JTable table) {
        table.setShowGrid(true);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // Use a font that has good Unicode character support
        table.setFont(new Font("Lucida Sans Unicode", Font.PLAIN, 12));
    }

    private void adjustColumnWidths(JTable table) {
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = 150; // Min width
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width + 10, width);
            }
            table.getColumnModel().getColumn(column).setPreferredWidth(width);
        }
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
                            dataFilePanel.getHeaderRowIndices(),
                            dataFilePanel.getConcatenationMode(),
                            rules
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
                        JOptionPane.showMessageDialog((Frame) SwingUtilities.getWindowAncestor(FilterPanel.this), "Results exported successfully!", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        logger.error("Export failed.", e);
                        JOptionPane.showMessageDialog((Frame) SwingUtilities.getWindowAncestor(FilterPanel.this), "Failed to export results: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void createFilterFromSelection() {
        int[] selectedRows = filterValuesPreviewTable.getSelectedRows();
        int[] selectedCols = filterValuesPreviewTable.getSelectedColumns();

        if (selectedRows.length == 0 || selectedCols.length == 0) {
            JOptionPane.showMessageDialog(this, "Please select one or more cells in the 'Filter Values' table first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> targetColumns = dataFilePanel.getColumnNames();
        if (targetColumns.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Could not retrieve column names from the data file. Please ensure it is loaded correctly.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        FilterTargetDialog targetDialog = new FilterTargetDialog((Frame) SwingUtilities.getWindowAncestor(this), targetColumns);
        targetDialog.setVisible(true);

        List<String> selectedTargets = targetDialog.getSelectedColumns();
        boolean trim = targetDialog.isTrimWhitespaceSelected();

        if (selectedTargets.isEmpty()) {
            return; // User cancelled target selection
        }

        for (int row : selectedRows) {
            for (int col : selectedCols) {
                Object cellValueObj = filterValuesPreviewTable.getValueAt(row, col);
                String cellValue = (cellValueObj == null) ? "" : cellValueObj.toString();
                String columnName = filterValuesPreviewTable.getColumnName(col);

                FilterSourceDialog sourceDialog = new FilterSourceDialog((Frame) SwingUtilities.getWindowAncestor(this), cellValue, columnName);
                sourceDialog.setVisible(true);

                FilterRule.SourceType sourceType = sourceDialog.getSelectedType();
                String sourceValue = sourceDialog.getSelectedValue();

                if (sourceType != null) {
                    for (String target : selectedTargets) {
                        FilterRule rule = new FilterRule(sourceType, sourceValue, target, trim);
                        logger.info("Creating new filter rule: {}", rule);
                        int rowIndex = filterRulesPanel.addRule(rule);
                        calculateAndDisplayCount(rule, rowIndex);
                    }
                }
            }
        }
    }

    private void calculateAndDisplayCount(FilterRule rule, int rowIndex) {
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return filteringService.countMatches(
                        dataFilePanel.getFilePath(),
                        dataFilePanel.getSelectedSheet(),
                        dataFilePanel.getHeaderRowIndices(),
                        dataFilePanel.getConcatenationMode(),
                        rule
                );
            }

            @Override
            protected void done() {
                try {
                    int count = get();
                    filterRulesPanel.updateRuleCount(rowIndex, count);
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Failed to count matches for rule: {}", rule, e);
                    filterRulesPanel.updateRuleCount(rowIndex, -1); // Indicate error
                }
            }
        }.execute();
    }

    private void loadPreviews() {
        // Data file can be large, so preview is fine
        loadTableData(dataFilePanel, dataPreviewModel, 50, "Error loading data preview", dataPreviewTable, true);
        // Filter values file should be read fully and accurately
        loadTableData(filterValuesFilePanel, filterValuesPreviewModel, -1, "Error loading filter values preview", filterValuesPreviewTable, false);
    }

    private void loadTableData(FilterFilePanel panel, DefaultTableModel model, int rowLimit, String errorTitle, JTable table, boolean useStreaming) {
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
                    return ExcelReader.read(filePath, sheetName, useStreaming);
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

                    List<String> headers = panel.getColumnNames();
                    Vector<String> headerVector = new Vector<>(headers);
                    model.setColumnIdentifiers(headerVector);

                    int headerRowCount = panel.getHeaderRowIndices().isEmpty() ? 1 : panel.getHeaderRowIndices().size();

                    Vector<Vector<Object>> dataVector = new Vector<>();
                    if (data.size() > headerRowCount) {
                        List<List<Object>> dataRows = data.subList(headerRowCount, data.size());
                        for(List<Object> row : dataRows) {
                            dataVector.add(new Vector<>(row));
                        }
                    }

                    model.setDataVector(dataVector, headerVector);
                    adjustColumnWidths(table);

                } catch (Exception e) {
                    JOptionPane.showMessageDialog((Frame) SwingUtilities.getWindowAncestor(FilterPanel.this), "Could not load data: " + e.getMessage(), errorTitle, JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
