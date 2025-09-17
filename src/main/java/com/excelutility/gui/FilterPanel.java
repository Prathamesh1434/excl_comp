package com.excelutility.gui;

import com.excelutility.core.FilterProfile;
import com.excelutility.core.FilterRule;
import com.excelutility.core.FilteringService;
import com.excelutility.core.expression.FilterExpression;
import com.excelutility.io.ExcelReader;
import com.excelutility.io.FilterProfileService;
import com.excelutility.io.SimpleExcelWriter;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;
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
    private final FilterExpressionBuilderPanel filterExpressionBuilderPanel;
    private final FilteringService filteringService = new FilteringService();
    private final JLabel totalMatchesLabel;
    private JTabbedPane resultTabs;

    private enum ProcessDestination { VIEW, EXPORT, CALCULATE_ONLY }

    private Color selectedColor = Color.YELLOW;
    private final AppContainer appContainer;
    private final FilterProfileService profileService = new FilterProfileService();

    public FilterPanel(AppContainer appContainer) {
        this.appContainer = appContainer;
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

        // --- Main Content Panel ---
        JPanel mainContentPanel = new JPanel(new MigLayout("fill, insets 5", "[grow, fill]", "[grow 50][grow 50]"));
        add(mainContentPanel, BorderLayout.CENTER);

        // --- Top Row: Previews and Results ---
        JPanel topContentPanel = new JPanel(new MigLayout("fill, insets 0", "[grow 33][grow 33][grow 34]"));

        // Data Preview Table
        dataPreviewModel = new DefaultTableModel();
        dataPreviewTable = new JTable(dataPreviewModel);
        configureTable(dataPreviewTable);
        JScrollPane dataPreviewScroll = new JScrollPane(dataPreviewTable);
        dataPreviewScroll.setBorder(BorderFactory.createTitledBorder("Data Preview (First 50 rows)"));
        topContentPanel.add(dataPreviewScroll, "grow");

        // Filter Values Preview Table
        filterValuesPreviewModel = new DefaultTableModel();
        filterValuesPreviewTable = new JTable(filterValuesPreviewModel);
        configureTable(filterValuesPreviewTable);
        filterValuesPreviewTable.setCellSelectionEnabled(true);
        JScrollPane filterValuesPreviewScroll = new JScrollPane(filterValuesPreviewTable);
        filterValuesPreviewScroll.setBorder(BorderFactory.createTitledBorder("Filter Values Preview (Full Data)"));
        topContentPanel.add(filterValuesPreviewScroll, "grow");

        // Unified Data View and other results
        resultTabs = new JTabbedPane();
        JPanel unifiedDataViewPlaceholder = new JPanel(new BorderLayout()); // Using BorderLayout to easily add a table later
        unifiedDataViewPlaceholder.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        resultTabs.addTab("Unified Data View", unifiedDataViewPlaceholder);
        topContentPanel.add(resultTabs, "grow");

        mainContentPanel.add(topContentPanel, "grow, wrap");

        // --- Bottom Row: Builder and Actions ---
        JPanel bottomPanel = new JPanel(new MigLayout("fill", "[grow 70][grow 30]", "[grow]"));

        filterExpressionBuilderPanel = new FilterExpressionBuilderPanel(this);
        JScrollPane builderScrollPane = new JScrollPane(filterExpressionBuilderPanel);
        builderScrollPane.setBorder(BorderFactory.createTitledBorder("Filter Logic Builder"));
        bottomPanel.add(builderScrollPane, "grow");

        JPanel actionPanel = new JPanel(new MigLayout("wrap 1", "[grow]"));
        JButton colorButton = new JButton("Set Highlight Color");
        JButton downloadButton = new JButton("Download Filtered Results");

        JTextField searchField = new JTextField();
        actionPanel.add(new JLabel("Preview Search:"));
        actionPanel.add(searchField, "growx, wrap, gaptop 5");

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                searchTables(searchField.getText());
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                searchTables(searchField.getText());
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                searchTables(searchField.getText());
            }
        });

        actionPanel.add(colorButton, "growx, gaptop 10");

        JButton addGroupButton = new JButton("Add Group");
        actionPanel.add(addGroupButton, "growx, gaptop 10");

        JButton calculateButton = new JButton("Calculate Total");
        JButton viewButton = new JButton("View Overall Result");
        totalMatchesLabel = new JLabel("Total Matches: N/A");
        actionPanel.add(calculateButton, "split 3, gaptop 20");
        actionPanel.add(viewButton);
        actionPanel.add(totalMatchesLabel, "gapleft 10");

        actionPanel.add(downloadButton, "growx, gaptop 10");
        bottomPanel.add(actionPanel, "growy");

        mainContentPanel.add(bottomPanel, "grow");

        // Action Listeners
        previewButton.addActionListener(e -> loadPreviews());
        colorButton.addActionListener(e -> chooseColor());
        downloadButton.addActionListener(e -> startFilterProcess(ProcessDestination.EXPORT));
        viewButton.addActionListener(e -> startFilterProcess(ProcessDestination.VIEW));
        calculateButton.addActionListener(e -> startFilterProcess(ProcessDestination.CALCULATE_ONLY));

        addGroupButton.addActionListener(e -> {
            LogicalGroupPanel rootGroup = filterExpressionBuilderPanel.getRootGroup();
            ActionListener deleteListener = event -> {
                LogicalGroupPanel sourceGroup = (LogicalGroupPanel) event.getSource();
                rootGroup.removeComponent(sourceGroup);
            };
            String groupName = com.excelutility.core.AutoNamingService.suggestGroupName();
            LogicalGroupPanel newGroup = new LogicalGroupPanel(groupName, this, deleteListener);
            configureGroupPanel(newGroup); // Wire up the new group's "Add Rule" button
            rootGroup.addComponent(newGroup);
        });

        // The listener for creating filters is now attached to the buttons in the builder UI
        configureGroupPanel(filterExpressionBuilderPanel.getRootGroup());

        // We can still allow double-clicking on the table as a shortcut to add a rule to the root group
        filterValuesPreviewTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    createFilterFromSelection(filterExpressionBuilderPanel.getRootGroup());
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
     * Adds action listeners to the buttons of a group panel.
     * @param groupPanel The panel whose buttons need to be configured.
     */
    private void configureGroupPanel(LogicalGroupPanel groupPanel) {
        // Configure the "Add Rule" button for this group
        groupPanel.getAddRuleButton().addActionListener(e -> {
            createFilterFromSelection(groupPanel);
        });
    }

    /**
     * Initiates the filtering process for the overall expression.
     * @param destination The final action to take (View, Export, or just Calculate).
     */
    private void startFilterProcess(ProcessDestination destination) {
        com.excelutility.core.expression.FilterExpression expression = filterExpressionBuilderPanel.getRootGroup().getExpression();

        String dataFilePath = dataFilePanel.getFilePath();
        String sheetName = dataFilePanel.getSelectedSheet();
        if (dataFilePath == null || dataFilePath.trim().isEmpty() || sheetName == null) {
            JOptionPane.showMessageDialog(this, "Please select a data file and sheet first.", "Data File Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        totalMatchesLabel.setText("Total Matches: Calculating...");

        new SwingWorker<List<List<Object>>, Void>() {
            @Override
            protected List<List<Object>> doInBackground() throws Exception {
                return filteringService.filter(
                        dataFilePath,
                        sheetName,
                        dataFilePanel.getHeaderRowIndices(),
                        dataFilePanel.getConcatenationMode(),
                        expression
                );
            }

            @Override
            protected void done() {
                try {
                    List<List<Object>> filteredData = get();
                    int recordCount = filteredData.isEmpty() ? 0 : filteredData.size() - 1;
                    totalMatchesLabel.setText("Total Matches: " + recordCount);

                    if (destination == ProcessDestination.CALCULATE_ONLY) {
                        return; // We're done, just wanted the count
                    }

                    if (recordCount == 0) {
                        JOptionPane.showMessageDialog(FilterPanel.this, "No records match the filter criteria.", "No Matches", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    // For View or Export, we need to select columns
                    List<String> allColumns = dataFilePanel.getColumnNames();
                    ColumnSelectionDialog colDialog = new ColumnSelectionDialog((Frame) SwingUtilities.getWindowAncestor(FilterPanel.this), allColumns);
                    colDialog.setVisible(true);

                    if (colDialog.isCancelled()) {
                        return;
                    }
                    List<String> selectedColumns = colDialog.getSelectedColumns();
                    List<List<Object>> resultData = projectColumns(filteredData, selectedColumns);

                    if (destination == ProcessDestination.VIEW) {
                        // Display results in the "Unified Data View" tab
                        JPanel unifiedViewPanel = (JPanel) resultTabs.getComponentAt(0);
                        unifiedViewPanel.removeAll();
                        JTable resultTable = new JTable();
                        configureTable(resultTable);
                        JScrollPane scrollPane = new JScrollPane(resultTable);
                        unifiedViewPanel.add(scrollPane, BorderLayout.CENTER);

                        Vector<String> headers = new Vector<>(selectedColumns);
                        Vector<Vector<Object>> dataVector = new Vector<>();
                        for (int i = 1; i < resultData.size(); i++) { // Skip header row
                            dataVector.add(new Vector<>(resultData.get(i)));
                        }
                        resultTable.setModel(new DefaultTableModel(dataVector, headers));
                        adjustColumnWidths(resultTable);
                        resultTabs.setSelectedIndex(0);

                        unifiedViewPanel.revalidate();
                        unifiedViewPanel.repaint();

                    } else if (destination == ProcessDestination.EXPORT) {
                        promptAndSaveResults(resultData);
                    }

                } catch (Exception e) {
                    logger.error("Filtering process failed.", e);
                    totalMatchesLabel.setText("Total Matches: Error");
                    JOptionPane.showMessageDialog(FilterPanel.this, "Failed to apply filters: " + e.getCause().getMessage(), "Filtering Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public void previewRule(FilterRulePanel rulePanel) {
        runPreviewFilter(
            new com.excelutility.core.expression.RuleNode(rulePanel.getRule()),
            rulePanel::setRecordCount,
            rulePanel.getRuleName()
        );
    }

    public void previewGroup(LogicalGroupPanel groupPanel) {
        runPreviewFilter(
            groupPanel.getExpression(),
            groupPanel::setRecordCount,
            groupPanel.getGroupName()
        );
    }

    private void runPreviewFilter(FilterExpression expression, java.util.function.Consumer<Integer> countConsumer, String tabTitle) {
        String dataFilePath = dataFilePanel.getFilePath();
        String sheetName = dataFilePanel.getSelectedSheet();
        if (dataFilePath == null || dataFilePath.trim().isEmpty() || sheetName == null) {
            JOptionPane.showMessageDialog(this, "Please select a data file and sheet first.", "Data File Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<String> allColumns = dataFilePanel.getColumnNames();
        if (allColumns == null || allColumns.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Could not determine columns from data file.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new SwingWorker<List<List<Object>>, Void>() {
            @Override
            protected List<List<Object>> doInBackground() throws Exception {
                // For previews, we don't need to ask for columns, just show them all.
                List<List<Object>> filteredData = filteringService.filter(
                        dataFilePath,
                        sheetName,
                        dataFilePanel.getHeaderRowIndices(),
                        dataFilePanel.getConcatenationMode(),
                        expression
                );
                return projectColumns(filteredData, allColumns); // Project to all columns
            }

            @Override
            protected void done() {
                try {
                    List<List<Object>> resultData = get();
                    int recordCount = resultData.isEmpty() ? 0 : resultData.size() - 1;
                    countConsumer.accept(recordCount);
                    if (recordCount > 0) {
                        addPreviewTab(tabTitle, resultData);
                    } else {
                        JOptionPane.showMessageDialog(FilterPanel.this, "No records match the preview criteria.", "No Matches", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    logger.error("Preview filtering process failed.", e);
                    countConsumer.accept(-1); // Indicate error
                    JOptionPane.showMessageDialog(FilterPanel.this, "Failed to apply preview filter: " + e.getCause().getMessage(), "Filtering Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void addPreviewTab(String title, List<List<Object>> data) {
        JPanel contentPanel = new JPanel(new BorderLayout());
        JTable table = new JTable();
        configureTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        if (!data.isEmpty()) {
            Vector<String> headers = data.get(0).stream().map(Object::toString).collect(Collectors.toCollection(Vector::new));
            Vector<Vector<Object>> dataVector = new Vector<>();
            for (int i = 1; i < data.size(); i++) {
                dataVector.add(new Vector<>(data.get(i)));
            }
            table.setModel(new DefaultTableModel(dataVector, headers));
            adjustColumnWidths(table);
        }

        // Create a panel for the tab component (with a close button)
        JPanel tabComponent = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabComponent.setOpaque(false);
        JLabel tabLabel = new JLabel(title + " ");
        JButton closeButton = new JButton("x");
        closeButton.setMargin(new Insets(0, 2, 0, 2));
        closeButton.setToolTipText("Close this tab");

        tabComponent.add(tabLabel);
        tabComponent.add(closeButton);

        int tabIndex = resultTabs.getTabCount();
        resultTabs.insertTab(title, null, contentPanel, "Preview for " + title, tabIndex);
        resultTabs.setTabComponentAt(tabIndex, tabComponent);
        resultTabs.setSelectedIndex(tabIndex);

        closeButton.addActionListener(e -> resultTabs.remove(contentPanel));
    }

    private void promptAndSaveResults(List<List<Object>> filteredData) {
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

            try {
                Map<String, List<List<Object>>> results = Map.of("Filtered_Results", filteredData);
                SimpleExcelWriter.writeFilteredResults(filePath, results, true, selectedColor);
                int recordCount = filteredData.isEmpty() ? 0 : filteredData.size() - 1;
                JOptionPane.showMessageDialog(this, "Results exported successfully! " + recordCount + " records saved.", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                logger.error("Export failed.", e);
                JOptionPane.showMessageDialog(this, "Failed to export results: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void viewResultsForRule(FilterRule rule) {
        startPerRuleProcess(rule, false);
    }

    public void downloadResultsForRule(FilterRule rule) {
        startPerRuleProcess(rule, true);
    }

    private void startPerRuleProcess(FilterRule rule, boolean isExport) {
        logger.info("Starting per-rule process for rule: {}", rule);
        com.excelutility.core.expression.FilterExpression expression = new com.excelutility.core.expression.RuleNode(rule);

        String dataFilePath = dataFilePanel.getFilePath();
        String sheetName = dataFilePanel.getSelectedSheet();
        logger.info("Data file: {}, Sheet: {}", dataFilePath, sheetName);
        if (dataFilePath == null || dataFilePath.trim().isEmpty() || sheetName == null) {
            JOptionPane.showMessageDialog(this, "Please select a data file and sheet first.", "Data File Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 1. Show column selection dialog
        List<String> allColumns = dataFilePanel.getColumnNames();
        logger.info("All available columns for selection: {}", allColumns);
        if (allColumns == null || allColumns.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Could not determine columns from data file.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        ColumnSelectionDialog colDialog = new ColumnSelectionDialog((Frame) SwingUtilities.getWindowAncestor(this), allColumns);
        colDialog.setVisible(true);

        if (colDialog.isCancelled()) {
            logger.warn("Column selection was cancelled.");
            return;
        }
        List<String> selectedColumns = colDialog.getSelectedColumns();
        logger.info("User selected columns: {}", selectedColumns);

        // 2. Run filtering in a background worker
        new SwingWorker<List<List<Object>>, Void>() {
            @Override
            protected List<List<Object>> doInBackground() throws Exception {
                List<List<Object>> filteredData = filteringService.filter(
                        dataFilePath,
                        sheetName,
                        dataFilePanel.getHeaderRowIndices(),
                        dataFilePanel.getConcatenationMode(),
                        expression
                );
                logger.info("Filtering service returned {} rows (including header).", filteredData.size());
                // Project to selected columns
                List<List<Object>> projectedData = projectColumns(filteredData, selectedColumns);
                logger.info("Projected data has {} rows (including header).", projectedData.size());
                if (!projectedData.isEmpty()) {
                    logger.info("Projected header: {}", projectedData.get(0));
                }
                return projectedData;
            }

            @Override
            protected void done() {
                try {
                    List<List<Object>> resultData = get();
                    if (isExport) {
                        promptAndSaveResults(resultData);
                    } else {
                        new ResultsViewerDialog((Frame) SwingUtilities.getWindowAncestor(FilterPanel.this), "Results for Rule: " + rule, resultData).setVisible(true);
                    }
                } catch (Exception e) {
                    logger.error("Per-rule filtering process failed.", e);
                    JOptionPane.showMessageDialog(FilterPanel.this, "Failed to apply rule: " + e.getCause().getMessage(), "Filtering Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private List<List<Object>> projectColumns(List<List<Object>> data, List<String> columnsToKeep) {
        if (data.isEmpty() || columnsToKeep.isEmpty()) {
            return data;
        }
        List<List<Object>> projectedData = new ArrayList<>();
        List<String> originalHeader = data.get(0).stream().map(Object::toString).collect(java.util.stream.Collectors.toList());
        List<Integer> indicesToKeep = new ArrayList<>();

        // Create a new header with only the columns to keep, in the order they were selected
        List<Object> newHeader = new ArrayList<>();
        for(String column : columnsToKeep) {
            int index = originalHeader.indexOf(column);
            if (index != -1) {
                indicesToKeep.add(index);
                newHeader.add(column);
            }
        }
        projectedData.add(newHeader);

        // Process data rows
        for (int i = 1; i < data.size(); i++) {
            List<Object> originalRow = data.get(i);
            List<Object> projectedRow = new ArrayList<>();
            for (int index : indicesToKeep) {
                projectedRow.add(index < originalRow.size() ? originalRow.get(index) : null);
            }
            projectedData.add(projectedRow);
        }
        return projectedData;
    }

    /**
     * Guides the user through creating filter rules from their table selection, one cell at a time.
     * The created rule is added to the specified target group.
     * @param targetGroup The {@link LogicalGroupPanel} to which the new rule should be added.
     */
    private void createFilterFromSelection(LogicalGroupPanel targetGroup) {
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

        // Iterate through each selected cell individually
        for (int row : selectedRows) {
            for (int col : selectedCols) {
                Object cellValueObj = filterValuesPreviewTable.getValueAt(row, col);
                String cellValue = (cellValueObj == null) ? "" : cellValueObj.toString();
                String columnName = filterValuesPreviewTable.getColumnName(col);

                String dialogTitle = String.format("Step 1/2: Select Target for Cell [%d, %d] (Value: %s)", row, col, cellValue);

                FilterTargetDialog targetDialog = new FilterTargetDialog((Frame) SwingUtilities.getWindowAncestor(this), targetColumns, dialogTitle);
                targetDialog.setVisible(true);

                if (targetDialog.isCancelled()) {
                    continue;
                }
                List<String> selectedTargets = targetDialog.getSelectedColumns();
                boolean trim = targetDialog.isTrimWhitespaceSelected();
                if (selectedTargets.isEmpty()) {
                    continue;
                }

                FilterSourceDialog sourceDialog = new FilterSourceDialog((Frame) SwingUtilities.getWindowAncestor(this), cellValue, columnName);
                sourceDialog.setVisible(true);

                FilterRule.SourceType sourceType = sourceDialog.getSelectedType();
                if (sourceType == null) {
                    continue;
                }
                String sourceValue = sourceDialog.getSelectedValue();

                for (String target : selectedTargets) {
                    FilterRule rule = new FilterRule(sourceType, sourceValue, target, trim);
                    logger.info("Creating new filter rule: {}", rule);
                    filterExpressionBuilderPanel.addRuleToGroup(targetGroup, rule);
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

    public JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem backItem = new JMenuItem("Back to Mode Selection");
        backItem.addActionListener(e -> appContainer.navigateTo("modeSelection"));
        fileMenu.add(backItem);
        fileMenu.addSeparator();

        JMenuItem saveProfileItem = new JMenuItem("Save Profile As...");
        saveProfileItem.addActionListener(e -> saveProfile());
        fileMenu.add(saveProfileItem);

        JMenuItem loadProfileItem = new JMenuItem("Load Profile...");
        loadProfileItem.addActionListener(e -> loadProfile());
        fileMenu.add(loadProfileItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        JMenu editMenu = new JMenu("Edit");
        JMenuItem profileManagerItem = new JMenuItem("Manage Profiles...");
        profileManagerItem.addActionListener(e -> manageProfiles());
        editMenu.add(profileManagerItem);
        menuBar.add(editMenu);

        return menuBar;
    }

    private void saveProfile() {
        FilterExpression expression = filterExpressionBuilderPanel.getRootGroup().getExpression();
        FilterProfile profile = new FilterProfile(expression);

        String profileName = JOptionPane.showInputDialog(this, "Enter a name for this profile:", "Save Filter Profile", JOptionPane.PLAIN_MESSAGE);
        if (profileName != null && !profileName.trim().isEmpty()) {
            try {
                profileService.saveProfile(profile, profileName);
                JOptionPane.showMessageDialog(this, "Profile '" + profileName + "' saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                logger.error("Failed to save filter profile: {}", profileName, e);
                JOptionPane.showMessageDialog(this, "Error saving profile: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadProfile() {
        List<String> profiles = profileService.getAvailableProfiles();
        if (profiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No saved filter profiles found.", "Load Profile", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String selectedProfile = (String) JOptionPane.showInputDialog(this, "Select a profile to load:",
                "Load Filter Profile", JOptionPane.QUESTION_MESSAGE, null, profiles.toArray(), profiles.get(0));

        if (selectedProfile != null) {
            try {
                FilterProfile loadedProfile = profileService.loadProfile(selectedProfile);
                rebuildUIFromProfile(loadedProfile);
                JOptionPane.showMessageDialog(this, "Profile '" + selectedProfile + "' loaded successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                logger.error("Failed to load filter profile: {}", selectedProfile, e);
                JOptionPane.showMessageDialog(this, "Error loading profile: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void rebuildUIFromProfile(FilterProfile profile) {
        // Clear the entire current UI
        filterExpressionBuilderPanel.getRootGroup().removeAll();
        com.excelutility.core.AutoNamingService.reset();

        // Recursively build the new UI from the loaded profile
        populateGroupFromNode(filterExpressionBuilderPanel.getRootGroup(), (com.excelutility.core.expression.GroupNode) profile.getRootExpression());

        filterExpressionBuilderPanel.revalidate();
        filterExpressionBuilderPanel.repaint();
    }

    private void populateGroupFromNode(LogicalGroupPanel uiGroup, com.excelutility.core.expression.GroupNode dataNode) {
        uiGroup.setGroupName(dataNode.getName());
        // setOperator is no longer needed

        for (FilterExpression childNode : dataNode.getChildren()) {
            if (childNode instanceof com.excelutility.core.expression.RuleNode) {
                com.excelutility.core.expression.RuleNode ruleNode = (com.excelutility.core.expression.RuleNode) childNode;
                filterExpressionBuilderPanel.addRuleToGroup(uiGroup, ruleNode.getRule());
            } else if (childNode instanceof com.excelutility.core.expression.GroupNode) {
                com.excelutility.core.expression.GroupNode childGroupNode = (com.excelutility.core.expression.GroupNode) childNode;

                ActionListener deleteListener = event -> {
                    LogicalGroupPanel sourceGroup = (LogicalGroupPanel) event.getSource();
                    uiGroup.removeComponent(sourceGroup);
                };

                LogicalGroupPanel newUiGroup = new LogicalGroupPanel(childGroupNode.getName(), this, deleteListener);
                configureGroupPanel(newUiGroup); // Make sure the new group's buttons are wired up
                uiGroup.addComponent(newUiGroup);

                // Recurse
                populateGroupFromNode(newUiGroup, childGroupNode);
            }
        }
    }

    private void manageProfiles() {
        JOptionPane.showMessageDialog(this, "Profile Manager is not yet implemented.", "Not Implemented", JOptionPane.INFORMATION_MESSAGE);
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
