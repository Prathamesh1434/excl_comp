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
import javax.swing.JOptionPane;
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
 * The main panel for the "SPEC QA Recon" mode.
 * This class orchestrates the entire filtering workflow, from file selection to exporting results.
 */
public class FilterPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(FilterPanel.class);

    private final FilterFilePanel dataFilePanel;
    private final FilterFilePanel filterValuesFilePanel;
    private JTable dataPreviewTable; // Kept for compatibility with loadTableData, but not added to UI
    private final JTable filterValuesPreviewTable;
    private final DefaultTableModel dataPreviewModel;
    private final DefaultTableModel filterValuesPreviewModel;
    private final FilterExpressionBuilderPanel filterExpressionBuilderPanel;
    private final JTable finalResultsTable;
    private final DefaultTableModel finalResultsModel;
    private final JTable previewResultsTable;
    private final DefaultTableModel previewResultsModel;
    private final JTable allResultsTable;
    private final DefaultTableModel allResultsModel;
    private final JTabbedPane resultsTabbedPane;
    private final FilteringService filteringService = new FilteringService();
    private final JLabel totalMatchesLabel;

    private enum ProcessDestination { VIEW, EXPORT, CALCULATE_ONLY, PREVIEW }

    private Color selectedColor = Color.YELLOW;
    private java.util.List<java.util.List<Object>> lastPreviewResults;
    private java.util.List<java.util.List<Object>> lastFinalResults;
    private final AppContainer appContainer;
    private final FilterProfileService profileService = new FilterProfileService();

    public FilterPanel(AppContainer appContainer) {
        this.appContainer = appContainer;
        // Main layout with a top panel for file selection and a bottom panel for the two-column layout
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Top Panel for File Selection ---
        JPanel topPanel = new JPanel(new MigLayout("fillx", "[grow][grow]"));
        dataFilePanel = new FilterFilePanel("Data File (to be filtered)", this);
        filterValuesFilePanel = new FilterFilePanel("Filter Values File", this);
        topPanel.add(dataFilePanel, "growx");
        topPanel.add(filterValuesFilePanel, "growx, wrap");

        JButton previewButton = new JButton("Load & Preview Files");
        topPanel.add(previewButton, "span, center");
        add(topPanel, BorderLayout.NORTH);

        // --- Main Content Panel with Two Columns ---
        JPanel mainContentPanel = new JPanel(new MigLayout("fill, ins 0", "[35%][65%]", "[grow]"));
        add(mainContentPanel, BorderLayout.CENTER);

        // --- Left Column: Controls ---
        JPanel leftColumnPanel = new JPanel(new MigLayout("wrap 1, fillx", "[grow]"));
        mainContentPanel.add(leftColumnPanel, "growy");

        // --- Right Column: Unified Data View ---
        JPanel rightColumnPanel = new JPanel(new MigLayout("fill, ins 0", "[grow]", "[grow]"));
        mainContentPanel.add(rightColumnPanel, "grow");


        // --- Populate Left Column ---

        // 1. Filter Logic Builder Card
        filterExpressionBuilderPanel = new FilterExpressionBuilderPanel(this);
        JScrollPane builderScroll = new JScrollPane(filterExpressionBuilderPanel);
        builderScroll.setBorder(BorderFactory.createTitledBorder("Filter Logic Builder"));
        builderScroll.getVerticalScrollBar().setUnitIncrement(16);
        leftColumnPanel.add(builderScroll, "grow, h 60%");

        // 2. Filter Actions Card
        JPanel filterActionsCard = new JPanel(new MigLayout("wrap 1, fillx", "[grow]"));
        filterActionsCard.setBorder(BorderFactory.createTitledBorder("Filter Actions"));
        totalMatchesLabel = new JLabel("Total Matches: N/A");
        JButton previewSearchButton = new JButton("Preview Search");
        JButton calculateButton = new JButton("Calculate Total");
        JButton viewButton = new JButton("View Overall Result");
        JButton downloadButton = new JButton("Download Filtered Results");
        JButton colorButton = new JButton("Set Highlight Color");

        filterActionsCard.add(previewSearchButton, "growx");
        filterActionsCard.add(calculateButton, "split 2, gaptop 5");
        filterActionsCard.add(totalMatchesLabel, "gapleft 10, wrap");
        filterActionsCard.add(viewButton, "growx, gaptop 5");
        filterActionsCard.add(downloadButton, "growx, gaptop 5");
        filterActionsCard.add(colorButton, "growx, gaptop 10");
        leftColumnPanel.add(filterActionsCard, "growx");


        // --- Populate Right Column (Unified View) ---
        rightColumnPanel.setBorder(BorderFactory.createTitledBorder("Unified Data View"));
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplitPane.setResizeWeight(0.6);
        rightColumnPanel.add(rightSplitPane, "grow");

        // Top: Tabbed pane for results
        resultsTabbedPane = new JTabbedPane();

        // Final Results Table
        finalResultsModel = new DefaultTableModel();
        finalResultsTable = new JTable(finalResultsModel);
        configureTable(finalResultsTable);
        resultsTabbedPane.addTab("Final Result", new JScrollPane(finalResultsTable));

        // Preview Results Table
        previewResultsModel = new DefaultTableModel();
        previewResultsTable = new JTable(previewResultsModel);
        configureTable(previewResultsTable);
        resultsTabbedPane.addTab("Preview", new JScrollPane(previewResultsTable));

        // All Results Table
        allResultsModel = new DefaultTableModel();
        allResultsTable = new JTable(allResultsModel);
        configureTable(allResultsTable);
        resultsTabbedPane.addTab("All", new JScrollPane(allResultsTable));

        rightSplitPane.setTopComponent(resultsTabbedPane);

        // Bottom: Filter Values Preview Table (for creating filters)
        filterValuesPreviewModel = new DefaultTableModel();
        filterValuesPreviewTable = new JTable(filterValuesPreviewModel);
        configureTable(filterValuesPreviewTable);
        filterValuesPreviewTable.setCellSelectionEnabled(true);
        JScrollPane filterValuesPreviewScroll = new JScrollPane(filterValuesPreviewTable);
        filterValuesPreviewScroll.setBorder(BorderFactory.createTitledBorder("Filter Values Preview (Double-click to create filter)"));
        rightSplitPane.setBottomComponent(filterValuesPreviewScroll);

        // dataPreviewTable is no longer needed, but the model is still used by loadPreviews
        dataPreviewModel = new DefaultTableModel();
        dataPreviewTable = null; // Set to null to make it obvious it's not used


        // --- Action Listeners ---
        previewButton.addActionListener(e -> loadPreviews());
        previewSearchButton.addActionListener(e -> startFilterProcess(ProcessDestination.PREVIEW));
        downloadButton.addActionListener(e -> startFilterProcess(ProcessDestination.EXPORT));
        viewButton.addActionListener(e -> startFilterProcess(ProcessDestination.VIEW));
        calculateButton.addActionListener(e -> startFilterProcess(ProcessDestination.CALCULATE_ONLY));
        colorButton.addActionListener(e -> chooseColor());

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
     * Recursively adds action listeners to the buttons of a group panel and its future children.
     * @param groupPanel The panel whose buttons need to be configured.
     */
    private void configureGroupPanel(LogicalGroupPanel groupPanel) {
        // Configure the "Add Rule" button for this group
        groupPanel.getAddRuleButton().addActionListener(e -> {
            createFilterFromSelection(groupPanel);
        });

        // Configure the "Add Group" button for this group
        groupPanel.getAddGroupButton().addActionListener(e -> {
            // The delete listener for the new subgroup will remove it from its parent (this groupPanel)
            ActionListener deleteListener = event -> {
                LogicalGroupPanel sourceGroup = (LogicalGroupPanel) event.getSource();
                groupPanel.removeComponent(sourceGroup);
            };

            String groupName = com.excelutility.core.AutoNamingService.suggestGroupName();
            LogicalGroupPanel newGroup = new LogicalGroupPanel(groupName, deleteListener);

            // IMPORTANT: Recursively configure the new group's buttons before adding it
            configureGroupPanel(newGroup);

            groupPanel.addComponent(newGroup);
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
                        lastFinalResults = resultData;
                        populateTable(finalResultsModel, finalResultsTable, resultData);
                        resultsTabbedPane.setSelectedIndex(0); // Switch to "Final Result" tab
                        updateAllResultsTab();
                    } else if (destination == ProcessDestination.PREVIEW) {
                        lastPreviewResults = resultData;
                        populateTable(previewResultsModel, previewResultsTable, resultData);
                        resultsTabbedPane.setSelectedIndex(1); // Switch to "Preview" tab
                        updateAllResultsTab();
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
                        populateTable(previewResultsModel, previewResultsTable, resultData);
                        resultsTabbedPane.setSelectedIndex(1); // Switch to "Preview" tab
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

    private void updateAllResultsTab() {
        // Combine headers
        List<String> headers = new ArrayList<>();
        if (lastFinalResults != null && !lastFinalResults.isEmpty()) {
            headers.addAll(lastFinalResults.get(0).stream().map(Object::toString).collect(Collectors.toList()));
        } else if (lastPreviewResults != null && !lastPreviewResults.isEmpty()) {
            headers.addAll(lastPreviewResults.get(0).stream().map(Object::toString).collect(Collectors.toList()));
        } else {
            allResultsModel.setDataVector(new Vector<>(), new Vector<>());
            return;
        }

        Vector<String> headerVector = new Vector<>(headers);
        Vector<Vector<Object>> dataVector = new Vector<>();

        // Add final results first
        if (lastFinalResults != null && lastFinalResults.size() > 1) {
            for (int i = 1; i < lastFinalResults.size(); i++) {
                Vector<Object> row = new Vector<>(lastFinalResults.get(i));
                row.add(0, "Final"); // Add a hidden identifier
                dataVector.add(row);
            }
        }
        // Add preview results
        if (lastPreviewResults != null && lastPreviewResults.size() > 1) {
            for (int i = 1; i < lastPreviewResults.size(); i++) {
                 Vector<Object> row = new Vector<>(lastPreviewResults.get(i));
                row.add(0, "Preview"); // Add a hidden identifier
                dataVector.add(row);
            }
        }

        headerVector.add(0, "ResultType");
        allResultsModel.setDataVector(dataVector, headerVector);

        // Hide the identifier column
        allResultsTable.getColumnModel().removeColumn(allResultsTable.getColumnModel().getColumn(0));
        allResultsTable.setDefaultRenderer(Object.class, new HighlightingTableCellRenderer());
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
        String profileName = JOptionPane.showInputDialog(this, "Enter a name for this profile:", "Save Filter Profile", JOptionPane.PLAIN_MESSAGE);
        if (profileName == null || profileName.trim().isEmpty()) {
            return; // User cancelled or entered empty name
        }

        FilterExpression expression = filterExpressionBuilderPanel.getRootGroup().getExpression();
        String dataSheet = dataFilePanel.getSelectedSheet();
        List<String> columns = dataFilePanel.getColumnNames();

        if (dataSheet == null || columns.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cannot save profile without a loaded data file and selected sheet.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        FilterProfile profile = new FilterProfile(profileName.trim(), expression, dataSheet, columns);

        try {
            profileService.saveProfile(profile);
            JOptionPane.showMessageDialog(this, "Profile '" + profile.getProfileName() + "' saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            // Don't show a generic error if the user just cancelled the overwrite dialog
            if ("Save cancelled by user.".equals(e.getMessage())) {
                logger.info("Profile save cancelled by user for profile: {}", profile.getProfileName());
                return;
            }
            logger.error("Failed to save filter profile: {}", profile.getProfileName(), e);
            JOptionPane.showMessageDialog(this, "Error saving profile: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

        // Set the sheet and columns from the profile
        // This assumes the same data file is loaded, which is a reasonable expectation for a profile
        dataFilePanel.getSheetCombo().setSelectedItem(profile.getSelectedSheet());
        // We don't need to set columns here as they are part of the file itself.
        // We could potentially use profile.getDisplayColumns() to configure the view in the future.

        // Recursively build the new UI from the loaded profile
        populateGroupFromNode(filterExpressionBuilderPanel.getRootGroup(), (com.excelutility.core.expression.GroupNode) profile.getRootExpression());

        filterExpressionBuilderPanel.revalidate();
        filterExpressionBuilderPanel.repaint();
    }

    private void populateGroupFromNode(LogicalGroupPanel uiGroup, com.excelutility.core.expression.GroupNode dataNode) {
        uiGroup.setGroupName(dataNode.getName());
        uiGroup.setOperator(dataNode.getOperator());

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

                LogicalGroupPanel newUiGroup = new LogicalGroupPanel(childGroupNode.getName(), deleteListener);
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

    private void loadPreviews() {
        // The data file preview is no longer shown, but we can still load the filter values
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
                    if (table != null) {
                        adjustColumnWidths(table);
                    }

                } catch (Exception e) {
                    JOptionPane.showMessageDialog((Frame) SwingUtilities.getWindowAncestor(FilterPanel.this), "Could not load data: " + e.getMessage(), errorTitle, JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void populateTable(DefaultTableModel model, JTable table, List<List<Object>> data) {
        if (data == null || data.isEmpty()) {
            model.setDataVector(new Vector<>(), new Vector<>());
            return;
        }

        Vector<String> headerVector = new Vector<>(data.get(0).stream().map(Object::toString).collect(Collectors.toList()));
        model.setColumnIdentifiers(headerVector);

        Vector<Vector<Object>> dataVector = new Vector<>();
        if (data.size() > 1) {
            List<List<Object>> dataRows = data.subList(1, data.size());
            for (List<Object> row : dataRows) {
                dataVector.add(new Vector<>(row));
            }
        }
        model.setDataVector(dataVector, headerVector);
        adjustColumnWidths(table);
    }

    /**
     * A custom cell renderer that highlights rows based on their source (Preview or Final).
     */
    private static class HighlightingTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        private static final Color PREVIEW_COLOR = new Color(220, 240, 255); // Light blue
        private static final Color FINAL_COLOR = new Color(220, 255, 220);   // Light green

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                // The "ResultType" is in the first column of the model, which is hidden from the view
                String resultType = (String) table.getModel().getValueAt(table.convertRowIndexToModel(row), 0);
                if ("Preview".equals(resultType)) {
                    c.setBackground(PREVIEW_COLOR);
                } else if ("Final".equals(resultType)) {
                    c.setBackground(FINAL_COLOR);
                } else {
                    c.setBackground(table.getBackground());
                }
            }
            return c;
        }
    }
}
