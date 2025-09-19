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
    private final FilteringService filteringService = new FilteringService();
    private JLabel totalMatchesLabel;
    private JTabbedPane unifiedDataViewTabs;

    private enum ProcessDestination { VIEW, EXPORT, CALCULATE_ONLY }

    private Color selectedColor = Color.YELLOW;
    private final AppContainer appContainer;
    private final FilterProfileService profileService = new FilterProfileService();
    private final Map<String, Component> openPreviewTabs = new java.util.HashMap<>();

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
        JPanel mainContentPanel = new JPanel(new MigLayout("fill, insets 5",
                "[sg preview, grow, fill][sg preview, grow, fill][grow, fill]", // 3 columns
                "[grow 60, fill][grow 40, fill]")); // 2 rows
        add(mainContentPanel, BorderLayout.CENTER);

        // --- Top Row ---
        // Data Preview (Top-Left)
        dataPreviewModel = new DefaultTableModel();
        dataPreviewTable = new JTable(dataPreviewModel);
        configureTable(dataPreviewTable);
        JScrollPane dataPreviewScroll = new JScrollPane(dataPreviewTable);
        dataPreviewScroll.setBorder(BorderFactory.createTitledBorder("Data Preview (First 50 rows)"));
        mainContentPanel.add(dataPreviewScroll, "grow");

        // Filter Values Preview (Top-Center)
        filterValuesPreviewModel = new DefaultTableModel();
        filterValuesPreviewTable = new JTable(filterValuesPreviewModel);
        configureTable(filterValuesPreviewTable);
        filterValuesPreviewTable.setCellSelectionEnabled(true);
        JScrollPane filterValuesPreviewScroll = new JScrollPane(filterValuesPreviewTable);
        filterValuesPreviewScroll.setBorder(BorderFactory.createTitledBorder("Filter Values Preview (Full Data)"));
        mainContentPanel.add(filterValuesPreviewScroll, "grow");

        // Filter Logic Builder (Top-Right)
        filterExpressionBuilderPanel = new FilterExpressionBuilderPanel(this);
        JScrollPane builderScrollPane = new JScrollPane(filterExpressionBuilderPanel);
        builderScrollPane.setBorder(BorderFactory.createTitledBorder("Filter Logic Builder"));
        mainContentPanel.add(builderScrollPane, "grow, wrap"); // wrap to next row

        // --- Bottom Row ---
        // Unified Data View (Bottom-Left & Bottom-Center)
        unifiedDataViewTabs = new JTabbedPane();
        unifiedDataViewTabs.setBorder(BorderFactory.createTitledBorder("Unified Data View"));
        JPanel unifiedDataViewPlaceholder = new JPanel(new BorderLayout());
        unifiedDataViewTabs.addTab("Unified", unifiedDataViewPlaceholder);
        mainContentPanel.add(unifiedDataViewTabs, "span 2, grow");

        // Action Buttons (Bottom-Right)
        JPanel actionPanel = new JPanel(new MigLayout("wrap 1, fillx, insets 10", "[grow, fill]"));
        actionPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
        JButton addGroupButton = new JButton("Add Group");
        JButton calculateButton = new JButton("Calculate Total");
        JButton viewButton = new JButton("View Overall Result");
        JButton downloadButton = new JButton("Download Filtered Results");
        JButton colorButton = new JButton("Set Highlight Color");
        totalMatchesLabel = new JLabel("Total Matches: N/A");
        actionPanel.add(addGroupButton);
        actionPanel.add(calculateButton, "gaptop 10");
        actionPanel.add(viewButton);
        actionPanel.add(totalMatchesLabel, "gaptop 5");
        actionPanel.add(downloadButton, "gaptop 10");
        actionPanel.add(colorButton);
        mainContentPanel.add(actionPanel, "grow");


        // --- Action Listeners ---
        previewButton.addActionListener(e -> loadPreviews());
        colorButton.addActionListener(e -> chooseColor());
        downloadButton.addActionListener(e -> promptAndSaveResults());
        viewButton.addActionListener(e -> startFilterProcess(ProcessDestination.VIEW));
        calculateButton.addActionListener(e -> startFilterProcess(ProcessDestination.CALCULATE_ONLY));

        addGroupButton.addActionListener(e -> {
            LogicalGroupPanel rootGroup = filterExpressionBuilderPanel.getRootGroup();
            ActionListener changeListener = ev -> updateFilterResults();
            ActionListener deleteListener = event -> {
                LogicalGroupPanel sourceGroup = (LogicalGroupPanel) event.getSource();
                rootGroup.removeComponent(sourceGroup);
                updateFilterResults();
            };
            String groupName = com.excelutility.core.AutoNamingService.suggestGroupName();
            LogicalGroupPanel newGroup = new LogicalGroupPanel(groupName, deleteListener, changeListener);
            newGroup.getAddRuleButton().addActionListener(ev -> createFilterFromSelection(newGroup));
            newGroup.getPreviewButton().addActionListener(ev -> previewGroup(newGroup));
            rootGroup.addComponent(newGroup);
            updateGroupCount(newGroup);
        });

        filterExpressionBuilderPanel.getRootGroup().getPreviewButton().addActionListener(e -> previewGroup(filterExpressionBuilderPanel.getRootGroup()));

        filterExpressionBuilderPanel.getRootGroup().getAddRuleButton().addActionListener(ev -> createFilterFromSelection(filterExpressionBuilderPanel.getRootGroup()));

        filterValuesPreviewTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    createFilterFromSelection(filterExpressionBuilderPanel.getRootGroup());
                }
            }
        });
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

    public void updateFilterResults() {
        // First, update the main view.
        startFilterProcess(ProcessDestination.VIEW);

        // Then, iterate through all groups and update their individual counts.
        List<LogicalGroupPanel> allGroups = filterExpressionBuilderPanel.getRootGroup().getAllGroupPanels();
        for (LogicalGroupPanel group : allGroups) {
            updateGroupCount(group);
        }
    }

    private void updateGroupCount(LogicalGroupPanel groupPanel) {
        String dataFilePath = dataFilePanel.getFilePath();
        String sheetName = dataFilePanel.getSelectedSheet();
        if (dataFilePath == null || dataFilePath.trim().isEmpty() || sheetName == null) {
            return; // Can't calculate without a data file
        }

        FilterExpression expression = groupPanel.getExpression();

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return filteringService.getMatchCount(dataFilePath, sheetName, dataFilePanel.getHeaderRowIndices(), dataFilePanel.getConcatenationMode(), expression);
            }

            @Override
            protected void done() {
                try {
                    int count = get();
                    groupPanel.setRecordCount(count);
                } catch (Exception e) {
                    logger.error("Failed to update group count for group '{}'", groupPanel.getName(), e);
                    groupPanel.setRecordCount(-1); // Indicate an error
                }
            }
        }.execute();
    }

    private void startFilterProcess(ProcessDestination destination) {
        FilterExpression expression = filterExpressionBuilderPanel.getRootGroup().getExpression();
        String dataFilePath = dataFilePanel.getFilePath();
        String sheetName = dataFilePanel.getSelectedSheet();
        if (dataFilePath == null || dataFilePath.trim().isEmpty() || sheetName == null) {
            if (destination != ProcessDestination.CALCULATE_ONLY) {
                JOptionPane.showMessageDialog(this, "Please select a data file and sheet first.", "Data File Required", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        totalMatchesLabel.setText("Total Matches: Calculating...");

        new SwingWorker<List<List<Object>>, Void>() {
            @Override
            protected List<List<Object>> doInBackground() throws Exception {
                return filteringService.filter(dataFilePath, sheetName, dataFilePanel.getHeaderRowIndices(), dataFilePanel.getConcatenationMode(), expression);
            }

            @Override
            protected void done() {
                try {
                    List<List<Object>> filteredData = get();
                    int recordCount = filteredData.isEmpty() ? 0 : filteredData.size() - 1;
                    logger.info("FilterApply: expression='{}' -> matched={}", expression.getDescriptiveName(), recordCount);
                    totalMatchesLabel.setText("Total Matches: " + recordCount);

                    if (destination == ProcessDestination.CALCULATE_ONLY) {
                        return;
                    }

                    if (recordCount == 0 && destination != ProcessDestination.VIEW) {
                        JOptionPane.showMessageDialog(FilterPanel.this, "No records match the filter criteria.", "No Matches", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    List<String> allColumns = dataFilePanel.getColumnNames();
                    List<String> selectedColumns = allColumns;

                    if (destination == ProcessDestination.EXPORT) {
                        ColumnSelectionDialog colDialog = new ColumnSelectionDialog((Frame) SwingUtilities.getWindowAncestor(FilterPanel.this), allColumns);
                        colDialog.setVisible(true);
                        if (colDialog.isCancelled()) return;
                        selectedColumns = colDialog.getSelectedColumns();
                    }

                    List<List<Object>> resultData = projectColumns(filteredData, selectedColumns);

                    if (destination == ProcessDestination.VIEW) {
                        updateUnifiedViewTab(resultData, selectedColumns);
                    }
                } catch (Exception e) {
                    logger.error("Filtering process failed.", e);
                    totalMatchesLabel.setText("Total Matches: Error");
                    JOptionPane.showMessageDialog(FilterPanel.this, "Failed to apply filters: " + e.getCause().getMessage(), "Filtering Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void updateUnifiedViewTab(List<List<Object>> data, List<String> headers) {
        JPanel unifiedViewPanel = (JPanel) unifiedDataViewTabs.getComponentAt(0);
        unifiedViewPanel.removeAll();

        if (data.isEmpty() || data.size() <= 1) {
            unifiedViewPanel.add(new JLabel("No matching records found."), BorderLayout.CENTER);
        } else {
            JTable resultTable = new JTable();
            configureTable(resultTable);
            JScrollPane scrollPane = new JScrollPane(resultTable);
            unifiedViewPanel.add(scrollPane, BorderLayout.CENTER);
            Vector<String> headerVector = new Vector<>(headers);
            Vector<Vector<Object>> dataVector = new Vector<>();
            List<List<Object>> dataRows = data.subList(1, data.size());
            for (List<Object> row : dataRows) {
                dataVector.add(new Vector<>(row));
            }
            resultTable.setModel(new DefaultTableModel(dataVector, headerVector));
            adjustColumnWidths(resultTable);
        }
        unifiedViewPanel.revalidate();
        unifiedViewPanel.repaint();
        unifiedDataViewTabs.setSelectedIndex(0);
    }

    public void previewRule(FilterRulePanel rulePanel) {
        String tabTitle = rulePanel.getRuleName() + " - " + rulePanel.getRule().getDescriptiveName();
        // To prevent overly long tab titles, truncate if necessary
        if (tabTitle.length() > 30) {
            tabTitle = tabTitle.substring(0, 27) + "...";
        }
        runPreviewFilter(rulePanel.getExpression(), rulePanel::setRecordCount, tabTitle);
    }

    public void previewGroup(LogicalGroupPanel groupPanel) {
        String tabTitle = groupPanel.getName();
        logger.info("GroupPreviewOpen: name={}, expression='{}'", tabTitle, groupPanel.getExpression().getDescriptiveName());
        runPreviewFilter(groupPanel.getExpression(), groupPanel::setRecordCount, tabTitle);
    }

    private void runPreviewFilter(FilterExpression expression, java.util.function.Consumer<Integer> countConsumer, String tabTitle) {
        String dataFilePath = dataFilePanel.getFilePath();
        String sheetName = dataFilePanel.getSelectedSheet();
        if (dataFilePath == null || dataFilePath.trim().isEmpty() || sheetName == null) {
            JOptionPane.showMessageDialog(this, "Please select a data file and sheet first.", "Data File Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new SwingWorker<List<List<Object>>, Void>() {
            @Override
            protected List<List<Object>> doInBackground() throws Exception {
                return filteringService.filter(dataFilePath, sheetName, dataFilePanel.getHeaderRowIndices(), dataFilePanel.getConcatenationMode(), expression);
            }

            @Override
            protected void done() {
                try {
                    List<List<Object>> resultData = get();
                    int recordCount = resultData.isEmpty() ? 0 : resultData.size() - 1;
                    countConsumer.accept(recordCount);
                    addPreviewTab(tabTitle, resultData);
                } catch (Exception e) {
                    logger.error("Preview filtering process failed.", e);
                    countConsumer.accept(-1);
                    JOptionPane.showMessageDialog(FilterPanel.this, "Failed to apply preview filter: " + e.getCause().getMessage(), "Filtering Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void addPreviewTab(String title, List<List<Object>> data) {
        // If a tab with this title already exists, just focus it.
        if (openPreviewTabs.containsKey(title)) {
            unifiedDataViewTabs.setSelectedComponent(openPreviewTabs.get(title));
            return;
        }

        JPanel contentPanel = new JPanel(new BorderLayout());
        JTable table = new JTable();
        configureTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // Handle case where there are no results
        if (data.isEmpty() || data.size() <= 1) {
            DefaultTableModel model = new DefaultTableModel();
            if(!data.isEmpty()){ // Has headers but no rows
                 Vector<String> headers = data.get(0).stream().map(Object::toString).collect(Collectors.toCollection(Vector::new));
                 model.setColumnIdentifiers(headers);
            }
            model.addRow(new Vector<>() {{ add("No rows matched"); }});
            table.setModel(model);
        } else {
            Vector<String> headers = data.get(0).stream().map(Object::toString).collect(Collectors.toCollection(Vector::new));
            Vector<Vector<Object>> dataVector = new Vector<>();
            for (int i = 1; i < data.size(); i++) {
                dataVector.add(new Vector<>(data.get(i)));
            }
            table.setModel(new DefaultTableModel(dataVector, headers));
            adjustColumnWidths(table);
        }

        JPanel tabComponent = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabComponent.setOpaque(false);
        JLabel tabLabel = new JLabel(title + " ");
        JButton closeButton = new JButton("x");
        closeButton.setMargin(new Insets(0, 2, 0, 2));
        closeButton.setToolTipText("Close this tab");
        tabComponent.add(tabLabel);
        tabComponent.add(closeButton);

        int tabIndex = unifiedDataViewTabs.getTabCount();
        unifiedDataViewTabs.insertTab(title, null, contentPanel, "Preview for " + title, tabIndex);
        unifiedDataViewTabs.setTabComponentAt(tabIndex, tabComponent);
        unifiedDataViewTabs.setSelectedIndex(tabIndex);

        openPreviewTabs.put(title, contentPanel);

        closeButton.addActionListener(e -> {
            unifiedDataViewTabs.remove(contentPanel);
            openPreviewTabs.remove(title);
        });
    }

    private void promptAndSaveResults() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Filtered Results Workbook");
        chooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        chooser.setSelectedFile(new File("FilteredResults_" + timestamp + ".xlsx"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = chooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".xlsx")) {
                filePath = filePath + ".xlsx";
            }
            final String finalFilePath = filePath;

            // Gather all expressions
            Map<String, FilterExpression> expressions = new java.util.LinkedHashMap<>();
            expressions.put("Unified_Result", filterExpressionBuilderPanel.getRootGroup().getExpression());
            List<LogicalGroupPanel> groups = filterExpressionBuilderPanel.getRootGroup().getAllGroupPanels();
            groups.remove(filterExpressionBuilderPanel.getRootGroup()); // Exclude the root group itself
            for (int i = 0; i < groups.size(); i++) {
                LogicalGroupPanel group = groups.get(i);
                String safeName = SimpleExcelWriter.sanitizeSheetName("Group_" + (i + 1) + "_" + group.getName());
                expressions.put(safeName, group.getExpression());
            }

            logger.info("ExportStart: file={}, groupsCount={}, totalRows=?", finalFilePath, expressions.size() -1);

            new SwingWorker<Map<String, List<List<Object>>>, Void>() {
                @Override
                protected Map<String, List<List<Object>>> doInBackground() throws Exception {
                    return filteringService.filterMultiple(dataFilePanel.getFilePath(), dataFilePanel.getSelectedSheet(), dataFilePanel.getHeaderRowIndices(), dataFilePanel.getConcatenationMode(), expressions);
                }

                @Override
                protected void done() {
                    long startTime = System.currentTimeMillis();
                    try {
                        Map<String, List<List<Object>>> results = get();
                        SimpleExcelWriter.writeFilteredResults(finalFilePath, results, true, selectedColor);
                        long duration = System.currentTimeMillis() - startTime;
                        logger.info("ExportComplete: duration={}ms", duration);
                        JOptionPane.showMessageDialog(FilterPanel.this, "Results exported successfully!", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        logger.error("Export failed.", e);
                        JOptionPane.showMessageDialog(FilterPanel.this, "Failed to export results: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private List<List<Object>> projectColumns(List<List<Object>> data, List<String> columnsToKeep) {
        if (data.isEmpty() || columnsToKeep.isEmpty()) {
            return data;
        }
        List<List<Object>> projectedData = new ArrayList<>();
        List<String> originalHeader = data.get(0).stream().map(Object::toString).collect(Collectors.toList());
        List<Integer> indicesToKeep = new ArrayList<>();
        List<Object> newHeader = new ArrayList<>();
        for (String column : columnsToKeep) {
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
                if (targetDialog.isCancelled()) continue;
                List<String> selectedTargets = targetDialog.getSelectedColumns();
                boolean trim = targetDialog.isTrimWhitespaceSelected();
                if (selectedTargets.isEmpty()) continue;
                FilterSourceDialog sourceDialog = new FilterSourceDialog((Frame) SwingUtilities.getWindowAncestor(this), cellValue, columnName);
                sourceDialog.setVisible(true);
                FilterRule.SourceType sourceType = sourceDialog.getSelectedType();
                if (sourceType == null) continue;
                String sourceValue = sourceDialog.getSelectedValue();
                for (String target : selectedTargets) {
                    FilterRule rule = new FilterRule(sourceType, sourceValue, target, trim);
                    logger.info("FilterCreate: targetColumn='{}', op=EQUALS, value='{}', trim={}", target, sourceValue, trim);
                    filterExpressionBuilderPanel.addRuleToGroup(targetGroup, rule);
                }
                updateFilterResults();
            }
        }
    }

    private void loadPreviews() {
        loadTableData(dataFilePanel, dataPreviewModel, 50, "Error loading data preview", dataPreviewTable, true);
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
        filterExpressionBuilderPanel.getRootGroup().removeAll();
        com.excelutility.core.AutoNamingService.reset();
        populateGroupFromNode(filterExpressionBuilderPanel.getRootGroup(), (com.excelutility.core.expression.GroupNode) profile.getRootExpression());
        filterExpressionBuilderPanel.revalidate();
        filterExpressionBuilderPanel.repaint();
    }

    private void populateGroupFromNode(LogicalGroupPanel uiGroup, com.excelutility.core.expression.GroupNode dataNode) {
        // This logic needs to be updated to handle the new infix operator model.
        // For now, we will just load the rules and groups flatly.
        // A proper implementation would need to parse the expression tree and create OperatorPanels.
        uiGroup.setBorder(BorderFactory.createTitledBorder(dataNode.getName()));

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
                ActionListener changeListener = ev -> updateFilterResults();
                LogicalGroupPanel newUiGroup = new LogicalGroupPanel(childGroupNode.getName(), deleteListener, changeListener);
                newUiGroup.getAddRuleButton().addActionListener(e -> createFilterFromSelection(newUiGroup));
                newUiGroup.getPreviewButton().addActionListener(e -> previewGroup(newUiGroup));
                uiGroup.addComponent(newUiGroup);
                populateGroupFromNode(newUiGroup, childGroupNode);
            }
        }
    }

    private void manageProfiles() {
        JOptionPane.showMessageDialog(this, "Profile Manager is not yet implemented.", "Not Implemented", JOptionPane.INFORMATION_MESSAGE);
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
                        for (List<Object> row : dataRows) {
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
