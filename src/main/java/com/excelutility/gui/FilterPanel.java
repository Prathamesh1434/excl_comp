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

public class FilterPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(FilterPanel.class);

    private final FilterFilePanel dataFilePanel;
    private final FilterFilePanel filterValuesFilePanel;
    private final JTable dataPreviewTable;
    private final JTable filterValuesPreviewTable;
    private final DefaultTableModel dataPreviewModel;
    private final DefaultTableModel filterValuesPreviewModel;
    private final FilterExpressionBuilderPanel filterExpressionBuilderPanel;
    private final JTable resultsTable;
    private final DefaultTableModel resultsModel;
    private final FilteringService filteringService = new FilteringService();
    private final JLabel totalMatchesLabel;

    private enum ProcessDestination { VIEW, EXPORT, CALCULATE_ONLY }

    private Color selectedColor = Color.YELLOW;
    private final AppContainer appContainer;
    private final FilterProfileService profileService = new FilterProfileService();

    public FilterPanel(AppContainer appContainer) {
        this.appContainer = appContainer;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Top Panel for File Selection ---
        JPanel topPanel = new JPanel(new MigLayout("fillx", "[grow][grow]"));
        dataFilePanel = new FilterFilePanel("Data File (to be filtered)", this);
        filterValuesFilePanel = new FilterFilePanel("Filter Values File", this);
        topPanel.add(dataFilePanel, "growx");
        topPanel.add(filterValuesFilePanel, "growx, wrap");
        JButton loadFilesButton = new JButton("Load & Preview Files");
        loadFilesButton.setToolTipText("Load the selected sheets into the preview tabs below");
        topPanel.add(loadFilesButton, "span, center");
        add(topPanel, BorderLayout.NORTH);

        // --- Main Content Panel with Two Columns ---
        JPanel mainContentPanel = new JPanel(new MigLayout("fill, ins 0", "[40%][60%]", "[grow]"));
        add(mainContentPanel, BorderLayout.CENTER);

        // --- Left Column: Controls ---
        JPanel leftColumnPanel = new JPanel(new MigLayout("wrap 1, fillx", "[grow]"));
        mainContentPanel.add(leftColumnPanel, "growy");

        // --- Right Column: Unified Data View ---
        JPanel rightColumnPanel = new JPanel(new MigLayout("fill, ins 0", "[grow]", "[grow]"));
        mainContentPanel.add(rightColumnPanel, "grow");

        // --- Populate Left Column ---
        filterExpressionBuilderPanel = new FilterExpressionBuilderPanel(this);
        JScrollPane builderScroll = new JScrollPane(filterExpressionBuilderPanel);
        builderScroll.setBorder(BorderFactory.createTitledBorder("Filter Logic Builder"));
        builderScroll.getVerticalScrollBar().setUnitIncrement(16);
        leftColumnPanel.add(builderScroll, "grow");

        // --- Populate Right Column (Unified View) ---
        JTabbedPane rightTabbedPane = new JTabbedPane();
        rightColumnPanel.add(rightTabbedPane, "grow");

        dataPreviewModel = new DefaultTableModel();
        dataPreviewTable = new JTable(dataPreviewModel);
        configureTable(dataPreviewTable);
        rightTabbedPane.addTab("Data Preview", new JScrollPane(dataPreviewTable));

        filterValuesPreviewModel = new DefaultTableModel();
        filterValuesPreviewTable = new JTable(filterValuesPreviewModel);
        configureTable(filterValuesPreviewTable);
        filterValuesPreviewTable.setCellSelectionEnabled(true);
        rightTabbedPane.addTab("Filter Values Preview", new JScrollPane(filterValuesPreviewTable));

        resultsModel = new DefaultTableModel();
        resultsTable = new JTable(resultsModel);
        configureTable(resultsTable);
        rightTabbedPane.addTab("Results", new JScrollPane(resultsTable));

        // --- Bottom Action Bar ---
        JPanel bottomActionBar = new JPanel(new MigLayout("fillx, align center"));
        bottomActionBar.setBorder(BorderFactory.createEtchedBorder());
        add(bottomActionBar, BorderLayout.SOUTH);

        totalMatchesLabel = new JLabel("Total Matches: N/A");
        JButton runFilterButton = new JButton("Run Filter");
        runFilterButton.setToolTipText("Run the filter and show results in the 'Results' tab");
        JButton downloadButton = new JButton("Download Results");
        downloadButton.setToolTipText("Run the filter and save the results to an Excel file");
        JButton exitButton = new JButton("Exit");
        exitButton.setToolTipText("Exit the application");

        bottomActionBar.add(totalMatchesLabel, "gapright 20");
        bottomActionBar.add(runFilterButton, "sg actionButton");
        bottomActionBar.add(downloadButton, "sg actionButton");
        bottomActionBar.add(exitButton, "sg actionButton, gapleft 30");

        // --- Action Listeners ---
        loadFilesButton.addActionListener(e -> loadPreviews());
        runFilterButton.addActionListener(e -> startFilterProcess(ProcessDestination.VIEW));
        downloadButton.addActionListener(e -> startFilterProcess(ProcessDestination.EXPORT));
        exitButton.addActionListener(e -> System.exit(0));

        configureGroupPanel(filterExpressionBuilderPanel.getRootGroup());

        filterValuesPreviewTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    createFilterFromSelection(filterExpressionBuilderPanel.getRootGroup());
                }
            }
        });
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

    private void configureGroupPanel(LogicalGroupPanel groupPanel) {
        groupPanel.getAddRuleButton().addActionListener(e -> createFilterFromSelection(groupPanel));
        groupPanel.getAddGroupButton().addActionListener(e -> {
            ActionListener deleteListener = event -> {
                LogicalGroupPanel sourceGroup = (LogicalGroupPanel) event.getSource();
                groupPanel.removeComponent(sourceGroup);
            };
            String groupName = com.excelutility.core.AutoNamingService.suggestGroupName();
            LogicalGroupPanel newGroup = new LogicalGroupPanel(groupName, deleteListener);
            configureGroupPanel(newGroup);
            groupPanel.addComponent(newGroup);
        });
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

                    if (destination == ProcessDestination.CALCULATE_ONLY) {
                        return;
                    }

                    if (recordCount == 0) {
                        JOptionPane.showMessageDialog(FilterPanel.this, "No records match the filter criteria.", "No Matches", JOptionPane.INFORMATION_MESSAGE);
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
                        populateTable(resultsModel, resultsTable, resultData);
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

        FilterProfile profile = new FilterProfile(profileName.trim(), expression, dataSheet, columns);

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

        dataFilePanel.getSheetCombo().setSelectedItem(profile.getSelectedSheet());

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
                configureGroupPanel(newUiGroup);
                uiGroup.addComponent(newUiGroup);

                populateGroupFromNode(newUiGroup, childGroupNode);
            }
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
