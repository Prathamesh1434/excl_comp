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
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.regex.Pattern;
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

public class FilterPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(FilterPanel.class);

    private final FilterFilePanel dataFilePanel;
    private final FilterFilePanel filterValuesFilePanel;
    private final JTable dataPreviewTable;
    private final JTable filterValuesPreviewTable;
    private final DefaultTableModel dataPreviewModel;
    private final DefaultTableModel filterValuesPreviewModel;
    private final FilterExpressionBuilderPanel filterExpressionBuilderPanel;
    private final JTable consolidatedResultsTable;
    private final DefaultTableModel consolidatedResultsModel;
    private final JTabbedPane resultsTabbedPane;
    private final FilteringService filteringService = new FilteringService();
    private final JLabel totalMatchesLabel;

    private enum ProcessDestination { VIEW, EXPORT }

    private Color selectedColor = Color.YELLOW;
    private final AppContainer appContainer;
    private final FilterProfileService profileService = new FilterProfileService();

    public FilterPanel(AppContainer appContainer) {
        this.appContainer = appContainer;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Top Panel for File Selection ---
        JPanel topPanel = new JPanel(new MigLayout("fillx, ins 0", "[grow][grow]"));
        dataFilePanel = new FilterFilePanel("Data File (to be filtered)", this);
        filterValuesFilePanel = new FilterFilePanel("Filter Values File", this);
        topPanel.add(dataFilePanel, "growx");
        topPanel.add(filterValuesFilePanel, "growx, wrap");
        add(topPanel, BorderLayout.NORTH);

        // --- Main Content Split Pane ---
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplit.setResizeWeight(0.5);
        add(mainSplit, BorderLayout.CENTER);

        // --- Top part of Main Split Pane (Builder and Previews) ---
        JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        topSplit.setResizeWeight(0.5);
        mainSplit.setTopComponent(topSplit);

        // --- Left side: Filter Logic Builder ---
        filterExpressionBuilderPanel = new FilterExpressionBuilderPanel(this);
        JScrollPane builderScroll = new JScrollPane(filterExpressionBuilderPanel);
        builderScroll.setBorder(BorderFactory.createTitledBorder("Filter Logic Builder"));
        builderScroll.getVerticalScrollBar().setUnitIncrement(16);
        builderScroll.setMinimumSize(new Dimension(300, 200));
        topSplit.setLeftComponent(builderScroll);

        // --- Right side: Data Previews (Side-by-side) ---
        JPanel rightSidePanel = new JPanel(new BorderLayout(5, 5));
        rightSidePanel.setMinimumSize(new Dimension(300, 200));
        topSplit.setRightComponent(rightSidePanel);

        JSplitPane previewSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        previewSplit.setResizeWeight(0.5);
        rightSidePanel.add(previewSplit, BorderLayout.CENTER);

        dataPreviewModel = new DefaultTableModel();
        dataPreviewTable = new JTable(dataPreviewModel);
        configureTable(dataPreviewTable);
        JScrollPane dataPreviewScroll = new JScrollPane(dataPreviewTable);
        dataPreviewScroll.setBorder(BorderFactory.createTitledBorder("Data Preview"));
        previewSplit.setLeftComponent(dataPreviewScroll);

        filterValuesPreviewModel = new DefaultTableModel();
        filterValuesPreviewTable = new JTable(filterValuesPreviewModel);
        configureTable(filterValuesPreviewTable);
        filterValuesPreviewTable.setCellSelectionEnabled(true);
        JScrollPane filterValuesPreviewScroll = new JScrollPane(filterValuesPreviewTable);
        filterValuesPreviewScroll.setBorder(BorderFactory.createTitledBorder("Filter Values Preview"));
        previewSplit.setRightComponent(filterValuesPreviewScroll);

        JButton addFilterButton = new JButton("Add Filter from Selection");
        addFilterButton.setToolTipText("Create filter rules from the selected cells in the Filter Values Preview table");
        rightSidePanel.add(addFilterButton, BorderLayout.SOUTH);
        addFilterButton.addActionListener(e -> createFilterFromSelection(filterExpressionBuilderPanel.getRootGroup()));

        // --- Bottom part of Main Split Pane (Results) ---
        resultsTabbedPane = new JTabbedPane();
        resultsTabbedPane.setBorder(BorderFactory.createTitledBorder("Result Preview"));
        resultsTabbedPane.setMinimumSize(new Dimension(200, 150));
        mainSplit.setBottomComponent(resultsTabbedPane);

        consolidatedResultsModel = new DefaultTableModel();
        consolidatedResultsTable = new JTable(consolidatedResultsModel);
        configureTable(consolidatedResultsTable);
        resultsTabbedPane.addTab("Consolidated Results", new JScrollPane(consolidatedResultsTable));

        // --- Bottom Action Bar ---
        JPanel bottomActionBar = new JPanel(new MigLayout("fillx, ins 0", "[left]push[center]push[right]"));
        bottomActionBar.setBorder(BorderFactory.createEtchedBorder());
        add(bottomActionBar, BorderLayout.SOUTH);

        totalMatchesLabel = new JLabel("Total Matches: N/A");
        JButton loadFilesButton = new JButton("Load Previews");
        loadFilesButton.setToolTipText("Load the selected sheets into the preview panes above");
        JButton runFilterButton = new JButton("Run Filter");
        runFilterButton.setToolTipText("Run the filter and show results in the 'Result Preview' panel");
        JButton downloadButton = new JButton("Download Results");
        downloadButton.setToolTipText("Run the filter and save the results to an Excel file");
        JButton exitButton = new JButton("Exit");
        exitButton.setToolTipText("Exit the application");

        bottomActionBar.add(loadFilesButton, "sg actionButton, cell 0 0");
        bottomActionBar.add(runFilterButton, "sg actionButton, cell 1 0, split 3, center");
        bottomActionBar.add(downloadButton, "sg actionButton, center");
        bottomActionBar.add(totalMatchesLabel, "sg actionButton, center");
        bottomActionBar.add(exitButton, "sg actionButton, cell 2 0");

        // --- Action Listeners ---
        loadFilesButton.addActionListener(e -> loadPreviews());
        runFilterButton.addActionListener(e -> startFilterProcess(ProcessDestination.VIEW));
        downloadButton.addActionListener(e -> startFilterProcess(ProcessDestination.EXPORT));
        exitButton.addActionListener(e -> System.exit(0));

        configureGroupPanel(filterExpressionBuilderPanel.getRootGroup());
    }

    private void loadPreviews() {
        loadTableData(dataFilePanel, dataPreviewModel, 50, "Error loading data preview", dataPreviewTable);
        loadTableData(filterValuesFilePanel, filterValuesPreviewModel, -1, "Error loading filter values preview", filterValuesPreviewTable);
    }

    private void loadTableData(FilterFilePanel panel, DefaultTableModel model, int rowLimit, String errorTitle, JTable table) {
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
                    return ExcelReader.read(filePath, sheetName, false);
                }
            }

            @Override
            protected void done() {
                try {
                    List<List<Object>> data = get();
                    populateTable(model, table, data);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(FilterPanel.this, "Could not load data: " + e.getMessage(), errorTitle, JOptionPane.ERROR_MESSAGE);
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
            for (int i = 1; i < data.size(); i++) {
                dataVector.add(new Vector<>(data.get(i)));
            }
        }
        model.setDataVector(dataVector, headerVector);
        adjustColumnWidths(table);
    }

    private void configureTable(JTable table) {
        table.setShowGrid(true);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFont(new Font("Lucida Sans Unicode", Font.PLAIN, 12));
    }

    private void adjustColumnWidths(JTable table) {
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = 150;
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

    public void configureGroupPanel(LogicalGroupPanel groupPanel) {
        groupPanel.getAddRuleButton().addActionListener(e -> createFilterFromSelection(groupPanel));
    }

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

                    if (recordCount == 0) {
                        JOptionPane.showMessageDialog(FilterPanel.this, "No records match the filter criteria.", "No Matches", JOptionPane.INFORMATION_MESSAGE);
                        if (destination == ProcessDestination.VIEW) {
                            populateTable(consolidatedResultsModel, consolidatedResultsTable, new ArrayList<>());
                        }
                        return;
                    }

                    List<String> allColumns = dataFilePanel.getColumnNames();
                    ColumnSelectionDialog colDialog = new ColumnSelectionDialog((Frame) SwingUtilities.getWindowAncestor(FilterPanel.this), allColumns);
                    colDialog.setVisible(true);

                    if (colDialog.isCancelled()) {
                        return;
                    }
                    List<String> selectedColumns = colDialog.getSelectedColumns();
                    List<List<Object>> resultData = projectColumns(filteredData, selectedColumns);

                    if (destination == ProcessDestination.VIEW) {
                        populateTable(consolidatedResultsModel, consolidatedResultsTable, resultData);
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

    private List<List<Object>> projectColumns(List<List<Object>> data, List<String> columnsToKeep) {
        if (data.isEmpty() || columnsToKeep.isEmpty()) {
            return data;
        }
        List<List<Object>> projectedData = new ArrayList<>();
        List<String> originalHeader = data.get(0).stream().map(Object::toString).collect(java.util.stream.Collectors.toList());
        List<Integer> indicesToKeep = new ArrayList<>();

        List<Object> newHeader = new ArrayList<>();
        for(String column : columnsToKeep) {
            int index = originalHeader.indexOf(column);
            if (index != -1) {
                indicesToKeep.add(index);
                newHeader.add(column);
            }
        }
        projectedData.add(newHeader);

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

    public void calculateCountForRule(FilterRule rule, FilterRulePanel rulePanel) {
        FilterExpression expression = new com.excelutility.core.expression.RuleNode(rule);
        String dataFilePath = dataFilePanel.getFilePath();
        String sheetName = dataFilePanel.getSelectedSheet();

        if (dataFilePath == null || dataFilePath.trim().isEmpty() || sheetName == null) {
            // Can't calculate without a data file, maybe show a tooltip or a disabled count
            return;
        }

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                List<List<Object>> filteredData = filteringService.filter(
                        dataFilePath,
                        sheetName,
                        dataFilePanel.getHeaderRowIndices(),
                        dataFilePanel.getConcatenationMode(),
                        expression
                );
                return filteredData.isEmpty() ? 0 : filteredData.size() - 1; // Subtract header row
            }

            @Override
            protected void done() {
                try {
                    Integer count = get();
                    rulePanel.updateCount(count);
                } catch (Exception e) {
                    logger.error("Failed to calculate count for rule: {}", rule.getDescriptiveName(), e);
                    rulePanel.updateCount(-1); // Indicate an error
                }
            }
        }.execute();
    }

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

        // The user can click "Add Rule" on a group, or "Add Filter from Selection"
        // If they click "Add Rule", there is no selection, so we show a generic dialog.
        // If they click "Add Filter from Selection", we iterate.
        if (selectedRows.length == 0) {
             // This case is for when user clicks "Add Rule" on a group directly.
             // We can create a more generic rule creation dialog here if needed.
             // For now, we'll just prompt them to use the selection method.
            JOptionPane.showMessageDialog(this, "Please select one or more cells in the 'Filter Values' table first, then click 'Add Filter from Selection' below the table.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }


        // Iterate through each selected cell sequentially
        for (int row : selectedRows) {
            for (int col : selectedCols) {
                Object cellValueObj = filterValuesPreviewTable.getValueAt(row, col);
                String cellValue = (cellValueObj == null) ? "" : cellValueObj.toString().trim();
                if (cellValue.isEmpty()) {
                    continue; // Skip empty cells
                }
                String columnName = filterValuesPreviewTable.getColumnName(col);

                // --- Step 1: Source Dialog ---
                FilterSourceDialog sourceDialog = new FilterSourceDialog((Frame) SwingUtilities.getWindowAncestor(this), cellValue, columnName);
                sourceDialog.setVisible(true);

                if (sourceDialog.isCancelled()) {
                    // Allow user to cancel the whole sequence
                    int choice = JOptionPane.showConfirmDialog(this, "Do you want to stop adding more rules?", "Cancel Rule Creation", JOptionPane.YES_NO_OPTION);
                    if (choice == JOptionPane.YES_OPTION) {
                        return;
                    }
                    continue; // Skip this cell and move to the next
                }
                FilterRule.SourceType sourceType = sourceDialog.getSelectedType();
                String sourceValue = sourceDialog.getSelectedValue();

                // --- Step 2: Target Dialog ---
                String dialogTitle = String.format("Step 2/2: Select Target Column(s) for '%s'", sourceValue);
                FilterTargetDialog targetDialog = new FilterTargetDialog((Frame) SwingUtilities.getWindowAncestor(this), targetColumns, dialogTitle);
                targetDialog.setVisible(true);

                if (targetDialog.isCancelled()) {
                    continue; // Skip this rule
                }
                List<String> selectedTargets = targetDialog.getSelectedColumns();
                boolean trim = targetDialog.isTrimWhitespaceSelected();
                if (selectedTargets.isEmpty()) {
                    continue;
                }

                // --- Step 3: Create and Add Rule(s) ---
                for (String target : selectedTargets) {
                    FilterRule rule = new FilterRule(sourceType, sourceValue, target, trim);
                    logger.info("Creating new filter rule: {}", rule);
                    filterExpressionBuilderPanel.addRuleToGroup(targetGroup, rule);
                }
            }
        }
    }

    public void viewResultsForRule(FilterRule rule) {
        FilterExpression expression = new com.excelutility.core.expression.RuleNode(rule);
        String tabTitle = "Rule: " + rule.getDescriptiveName();
        runIndividualFilter(expression, tabTitle);
    }

    public void viewResultsForGroup(LogicalGroupPanel groupPanel) {
        FilterExpression expression = groupPanel.getExpression();
        String groupName = ((com.excelutility.core.expression.GroupNode) expression).getName();
        String tabTitle = "Group: " + groupName;
        runIndividualFilter(expression, tabTitle);
    }

    private void runIndividualFilter(FilterExpression expression, String tabTitle) {
        String dataFilePath = dataFilePanel.getFilePath();
        String sheetName = dataFilePanel.getSelectedSheet();
        if (dataFilePath == null || dataFilePath.trim().isEmpty() || sheetName == null) {
            JOptionPane.showMessageDialog(this, "Please select a data file and sheet first.", "Data File Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPanel placeholderPanel = new JPanel(new BorderLayout());
        placeholderPanel.add(new JLabel("Calculating results..."), BorderLayout.CENTER);
        resultsTabbedPane.addTab(tabTitle, placeholderPanel);
        final int tabIndex = resultsTabbedPane.getTabCount() - 1;
        resultsTabbedPane.setSelectedIndex(tabIndex);


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

                    List<String> allColumns = dataFilePanel.getColumnNames();
                    List<List<Object>> resultData = projectColumns(filteredData, allColumns);

                    DefaultTableModel newModel = new DefaultTableModel();
                    JTable newTable = new JTable(newModel);
                    configureTable(newTable);
                    populateTable(newModel, newTable, resultData);

                    ResultTabPanel resultTabPanel = new ResultTabPanel(newTable);
                    resultsTabbedPane.setComponentAt(tabIndex, resultTabPanel);

                    TabComponent tabComponent = new TabComponent(resultsTabbedPane);
                    tabComponent.setTitle(tabTitle);
                    resultsTabbedPane.setTabComponentAt(tabIndex, tabComponent);

                } catch (Exception e) {
                    logger.error("Individual filtering process failed.", e);
                    JPanel errorPanel = new JPanel(new BorderLayout());
                    errorPanel.add(new JLabel("Error: " + e.getMessage()), BorderLayout.CENTER);
                    resultsTabbedPane.setComponentAt(tabIndex, errorPanel);
                    JOptionPane.showMessageDialog(FilterPanel.this, "Failed to apply filter: " + e.getMessage(), "Filtering Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
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
            return;
        }

        FilterExpression expression = filterExpressionBuilderPanel.getRootGroup().getExpression();
        String dataSheet = dataFilePanel.getSelectedSheet();
        List<String> columns = dataFilePanel.getColumnNames();

        if (dataSheet == null || columns.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cannot save profile without a loaded data file and selected sheet.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        FilterProfile profile = new FilterProfile(profileName.trim(), dataFilePanel.getFilePath(), expression, dataSheet, columns);

        try {
            profileService.saveProfile(profile);
            JOptionPane.showMessageDialog(this, "Profile '" + profile.getProfileName() + "' saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
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
            loadProfile(selectedProfile);
        }
    }

    private void loadProfile(String profileName) {
        try {
            FilterProfile loadedProfile = profileService.loadProfile(profileName);
            rebuildUIFromProfile(loadedProfile);
            JOptionPane.showMessageDialog(this, "Profile '" + profileName + "' loaded successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            logger.error("Failed to load filter profile: {}", profileName, e);
            JOptionPane.showMessageDialog(this, "Error loading profile: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rebuildUIFromProfile(FilterProfile profile) {
        filterExpressionBuilderPanel.getRootGroup().removeAll();
        com.excelutility.core.AutoNamingService.reset();

        // Set file path first, which will trigger sheet loading
        if (profile.getSourceFilePath() != null && !profile.getSourceFilePath().isEmpty()) {
            dataFilePanel.setFilePath(profile.getSourceFilePath());
        }

        // Now set the selected sheet
        dataFilePanel.getSheetCombo().setSelectedItem(profile.getSelectedSheet());

        // Check for missing columns
        List<String> currentColumns = dataFilePanel.getColumnNames();
        if (profile.getDisplayColumns() != null) {
            List<String> missing = profile.getDisplayColumns().stream()
                    .filter(col -> !currentColumns.contains(col))
                    .collect(Collectors.toList());
            if (!missing.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Warning: The following columns from the saved profile were not found in the current data file:\n" +
                        String.join("\n", missing) +
                        "\n\nRules using these columns may not work correctly.",
                        "Profile Column Mismatch",
                        JOptionPane.WARNING_MESSAGE);
            }
        }

        populateGroupFromNode(filterExpressionBuilderPanel.getRootGroup(), (com.excelutility.core.expression.GroupNode) profile.getRootExpression());

        filterExpressionBuilderPanel.revalidate();
        filterExpressionBuilderPanel.repaint();
    }

    private void populateGroupFromNode(LogicalGroupPanel uiGroup, com.excelutility.core.expression.GroupNode dataNode) {
        // Unwrap the final named group to get to the structural expression
        if (dataNode.getChildren().size() == 1 && dataNode.getChildren().get(0) instanceof FilterExpression) {
             uiGroup.setGroupName(dataNode.getName());
             FilterExpression structuralExpression = dataNode.getChildren().get(0);

             List<FilterExpression> leaves = new ArrayList<>();
             List<FilteringService.LogicalOperator> operators = new ArrayList<>();

             flattenExpression(structuralExpression, leaves, operators);

             rebuildGroupUIFromFlattened(uiGroup, leaves, operators);
        } else { // Handle simple cases or old profile formats
            uiGroup.setGroupName(dataNode.getName());
            boolean isFirst = true;
            for(FilterExpression child : dataNode.getChildren()) {
                addLeafToUi(uiGroup, child, isFirst);
                isFirst = false;
            }
        }
    }

    private void flattenExpression(FilterExpression expression, List<FilterExpression> leaves, List<FilteringService.LogicalOperator> operators) {
        if (!(expression instanceof com.excelutility.core.expression.GroupNode)) {
            leaves.add(expression);
            return;
        }

        com.excelutility.core.expression.GroupNode node = (com.excelutility.core.expression.GroupNode) expression;

        // Heuristic: An unnamed group with two children is an intermediate connector node.
        if (node.getName().isEmpty() && node.getChildren().size() == 2) {
            flattenExpression(node.getChildren().get(0), leaves, operators);
            operators.add(node.getOperator());
            flattenExpression(node.getChildren().get(1), leaves, operators);
        } else {
            // This is a "leaf" in the context of the current group's logic (it might be a named group with its own internal logic).
            leaves.add(node);
        }
    }

    private void rebuildGroupUIFromFlattened(LogicalGroupPanel uiGroup, List<FilterExpression> leaves, List<FilteringService.LogicalOperator> operators) {
        if (leaves.isEmpty()) {
            return;
        }

        // Add the first leaf node
        addLeafToUi(uiGroup, leaves.get(0), true);

        // Add the subsequent connectors and leaves
        for (int i = 0; i < operators.size(); i++) {
            ConnectorPanel connector = new ConnectorPanel();
            connector.setOperator(operators.get(i));
            uiGroup.rebuildConnector(connector);

            addLeafToUi(uiGroup, leaves.get(i + 1), false);
        }
    }

    private void addLeafToUi(LogicalGroupPanel parentUiGroup, FilterExpression leaf, boolean isFirst) {
        if (leaf instanceof com.excelutility.core.expression.RuleNode) {
            // Rebuilding a rule does not need the isFirst flag, as addRuleToGroup uses addComponent, which handles connectors.
            // However, for programmatic rebuild, we need more control. Let's use rebuildComponent.
            ActionListener deleteListener = e -> {
                FilterRulePanel sourcePanel = (FilterRulePanel) e.getSource();
                parentUiGroup.removeComponent(sourcePanel);
            };
            FilterRulePanel newRulePanel = new FilterRulePanel(((com.excelutility.core.expression.RuleNode) leaf).getRule(), this, deleteListener);
            parentUiGroup.rebuildComponent(newRulePanel);
            calculateCountForRule(((com.excelutility.core.expression.RuleNode) leaf).getRule(), newRulePanel);

        } else if (leaf instanceof com.excelutility.core.expression.GroupNode) {
            com.excelutility.core.expression.GroupNode groupData = (com.excelutility.core.expression.GroupNode) leaf;

            ActionListener deleteListener = event -> {
                LogicalGroupPanel sourceGroup = (LogicalGroupPanel) event.getSource();
                parentUiGroup.removeComponent(sourceGroup);
            };

            LogicalGroupPanel newUiGroup = new LogicalGroupPanel(groupData.getName(), deleteListener, true, this);
            configureGroupPanel(newUiGroup);
            parentUiGroup.rebuildComponent(newUiGroup);

            // Recursively populate the new UI group with its children
            populateGroupFromNode(newUiGroup, groupData);
        }
    }

    private void manageProfiles() {
        FilterProfileManagerDialog dialog = new FilterProfileManagerDialog((Frame) SwingUtilities.getWindowAncestor(this), profileService);
        dialog.setVisible(true);

        String profileToLoad = dialog.getSelectedProfileForLoad();
        if (profileToLoad != null) {
            loadProfile(profileToLoad);
        }
    }
}
