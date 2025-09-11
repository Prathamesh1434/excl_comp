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

/**
 * The main panel for the "Filter Excel Data" mode.
 * This class orchestrates the entire filtering workflow, from file selection to exporting results.
 */
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

    private final JComboBox<FilteringService.LogicalOperator> logicalOperatorCombo;
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
        logicalOperatorCombo = new JComboBox<>(FilteringService.LogicalOperator.values());
        JButton colorButton = new JButton("Set Highlight Color");
        JButton downloadButton = new JButton("Download Filtered Results");

        JTextField searchField = new JTextField();
        JButton searchButton = new JButton("Search Previews");

        actionPanel.add(new JLabel("Preview Search:"), "split 2");
        actionPanel.add(searchField, "growx");
        actionPanel.add(searchButton, "wrap, gaptop 5");

        actionPanel.add(addFilterButton, "growx, gaptop 10");
        actionPanel.add(new JLabel("Filter Logic:"), "gaptop 10");
        actionPanel.add(logicalOperatorCombo, "growx");
        actionPanel.add(colorButton, "growx, gaptop 10");
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

    /**
     * Filters the preview tables based on the entered search text.
     * @param text The text to search for.
     */
    private void searchTables(String text) {
        if (text == null || text.trim().isEmpty()) {
            ((TableRowSorter) dataPreviewTable.getRowSorter()).setRowFilter(null);
            ((TableRowSorter) filterValuesPreviewTable.getRowSorter()).setRowFilter(null);
        } else {
            // Case-insensitive search
            RowFilter<Object, Object> rf = RowFilter.regexFilter("(?i)" + Pattern.quote(text));
            ((TableRowSorter) dataPreviewTable.getRowSorter()).setRowFilter(rf);
            ((TableRowSorter) filterValuesPreviewTable.getRowSorter()).setRowFilter(rf);
        }
    }

    /**
     * Applies common configuration to a JTable.
     * @param table The table to configure.
     */
    private void configureTable(JTable table) {
        table.setShowGrid(true);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // Use a font that has good Unicode character support
        table.setFont(new Font("Lucida Sans Unicode", Font.PLAIN, 12));
    }

    /**
     * Adjusts the column widths of a table to fit the content.
     * @param table The table whose columns to resize.
     */
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

    /**
     * Opens a color chooser dialog to select the highlight color for exported results.
     */
    private void chooseColor() {
        Color newColor = JColorChooser.showDialog(this, "Choose Highlight Color", selectedColor);
        if (newColor != null) {
            selectedColor = newColor;
        }
    }

    /**
     * Initiates the process of filtering the data and downloading the results to an Excel file.
     */
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
            final FilteringService.LogicalOperator operator = (FilteringService.LogicalOperator) logicalOperatorCombo.getSelectedItem();
            logger.info("Starting export. Operator: {}, Color: {}", operator, selectedColor);

            new SwingWorker<List<List<Object>>, Void>() {
                @Override
                protected List<List<Object>> doInBackground() throws Exception {
                    return filteringService.filter(
                            dataFilePanel.getFilePath(),
                            dataFilePanel.getSelectedSheet(),
                            dataFilePanel.getHeaderRowIndices(),
                            dataFilePanel.getConcatenationMode(),
                            rules,
                            operator
                    );
                }

                @Override
                protected void done() {
                    try {
                        List<List<Object>> filteredData = get();
                        int recordCount = filteredData.isEmpty() ? 0 : filteredData.size() - 1;
                        logger.info("Exporting {} records.", recordCount);

                        Map<String, List<List<Object>>> results = Map.of("Filtered_Results", filteredData);
                        SimpleExcelWriter.writeFilteredResults(finalFilePath, results, true, selectedColor);

                        JOptionPane.showMessageDialog((Frame) SwingUtilities.getWindowAncestor(FilterPanel.this), "Results exported successfully! " + recordCount + " records saved.", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        logger.error("Export failed.", e);
                        JOptionPane.showMessageDialog((Frame) SwingUtilities.getWindowAncestor(FilterPanel.this), "Failed to export results: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    /**
     * Guides the user through the process of creating one or more filter rules based on their table selection.
     */
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
                        filterRulesPanel.addRule(rule);
                    }
                }
            }
        }
    }

    /**
     * Kicks off the SwingWorkers to load the preview data for both selected files.
     */
    private void loadPreviews() {
        // Data file can be large, so preview is fine
        loadTableData(dataFilePanel, dataPreviewModel, 50, "Error loading data preview", dataPreviewTable, true);
        // Filter values file should be read fully and accurately
        loadTableData(filterValuesFilePanel, filterValuesPreviewModel, -1, "Error loading filter values preview", filterValuesPreviewTable, false);
    }

    /**
     * Loads data from an Excel file into a JTable model in a background thread.
     * @param panel The panel containing the file info.
     * @param model The table model to populate.
     * @param rowLimit The maximum number of rows to load (-1 for all).
     * @param errorTitle The title for any error dialogs.
     * @param table The table to adjust column widths for.
     * @param useStreaming Whether to use the memory-efficient streaming reader.
     */
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
