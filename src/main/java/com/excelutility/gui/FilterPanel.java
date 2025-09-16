package com.excelutility.gui;

import com.excelutility.core.FilterProfile;
import com.excelutility.core.FilterRule;
import com.excelutility.core.FilteringService;
import com.excelutility.core.expression.FilterExpression;
import com.excelutility.core.expression.GroupNode;
import com.excelutility.io.FilterProfileService;
import com.excelutility.io.SimpleExcelWriter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FilterPanel extends JPanel {

    private final FilterFilePanel dataFilePanel;
    private final FilterFilePanel filterValuesFilePanel;
    private final JTable dataPreviewTable;
    private final JTable filterValuesPreviewTable;
    private final DefaultTableModel dataPreviewModel;
    private final DefaultTableModel filterValuesPreviewModel;
    private final FilterExpressionBuilderPanel filterExpressionBuilderPanel;
    private final JTabbedPane resultsTabbedPane;
    private final FilteringService filteringService = new FilteringService();
    private final JLabel totalMatchesLabel;
    private final DefaultTableModel consolidatedResultsModel;
    private final JTable consolidatedResultsTable;
    private final AppContainer appContainer;
    private final FilterProfileService profileService = new FilterProfileService();

    private enum ProcessDestination { VIEW, EXPORT, CALCULATE_ONLY }

    public FilterPanel(AppContainer appContainer) {
        this.appContainer = appContainer;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Top Panel ---
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        JPanel fileSelectionPanel = new JPanel(new MigLayout("fillx, ins 0", "[grow][grow]"));
        dataFilePanel = new FilterFilePanel("Data File (to be filtered)", this);
        filterValuesFilePanel = new FilterFilePanel("Filter Values File", this);
        fileSelectionPanel.add(dataFilePanel, "growx");
        fileSelectionPanel.add(filterValuesFilePanel, "growx, wrap");
        topPanel.add(fileSelectionPanel, BorderLayout.CENTER);

        JButton previewButton = new JButton("Load & Preview Files");
        JPanel previewButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        previewButtonPanel.add(previewButton);
        topPanel.add(previewButtonPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // --- Center Split Pane ---
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setResizeWeight(0.5);
        add(mainSplit, BorderLayout.CENTER);

        // --- Left Side: Filter Logic Builder ---
        filterExpressionBuilderPanel = new FilterExpressionBuilderPanel(this);
        JScrollPane builderScroll = new JScrollPane(filterExpressionBuilderPanel);
        builderScroll.setBorder(BorderFactory.createTitledBorder("Filter Logic Builder"));
        builderScroll.setMinimumSize(new Dimension(300, 200));
        mainSplit.setLeftComponent(builderScroll);

        // --- Right Side: Data Previews ---
        JSplitPane previewSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        previewSplit.setResizeWeight(0.5);
        mainSplit.setRightComponent(previewSplit);

        // Data Preview Panel
        JPanel dataPreviewPanelContainer = new JPanel(new BorderLayout(5, 5));
        dataPreviewPanelContainer.setBorder(BorderFactory.createTitledBorder("Data Preview"));
        dataPreviewModel = new DefaultTableModel();
        dataPreviewTable = new JTable(dataPreviewModel);
        configureTable(dataPreviewTable);
        TableRowSorter<DefaultTableModel> dataSorter = new TableRowSorter<>(dataPreviewModel);
        dataPreviewTable.setRowSorter(dataSorter);
        JTextField dataSearchField = new JTextField();
        dataSearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateFilter(dataSearchField, dataSorter); }
            public void removeUpdate(DocumentEvent e) { updateFilter(dataSearchField, dataSorter); }
            public void insertUpdate(DocumentEvent e) { updateFilter(dataSearchField, dataSorter); }
        });
        JPanel dataSearchPanel = new JPanel(new MigLayout("insets 0, fillx"));
        dataSearchPanel.add(new JLabel("Search:"), "gapright 5");
        dataSearchPanel.add(dataSearchField, "growx");
        dataPreviewPanelContainer.add(dataSearchPanel, BorderLayout.NORTH);
        dataPreviewPanelContainer.add(new JScrollPane(dataPreviewTable), BorderLayout.CENTER);
        previewSplit.setLeftComponent(dataPreviewPanelContainer);

        // Filter Values Preview Panel
        JPanel filterValuesPreviewPanelContainer = new JPanel(new BorderLayout(5, 5));
        filterValuesPreviewPanelContainer.setBorder(BorderFactory.createTitledBorder("Filter Values Preview"));
        filterValuesPreviewModel = new DefaultTableModel();
        filterValuesPreviewTable = new JTable(filterValuesPreviewModel);
        configureTable(filterValuesPreviewTable);
        TableRowSorter<DefaultTableModel> filterValuesSorter = new TableRowSorter<>(filterValuesPreviewModel);
        filterValuesPreviewTable.setRowSorter(filterValuesSorter);
        JTextField filterValuesSearchField = new JTextField();
        filterValuesSearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateFilter(filterValuesSearchField, filterValuesSorter); }
            public void removeUpdate(DocumentEvent e) { updateFilter(filterValuesSearchField, filterValuesSorter); }
            public void insertUpdate(DocumentEvent e) { updateFilter(filterValuesSearchField, filterValuesSorter); }
        });
        JPanel fvSearchPanel = new JPanel(new MigLayout("insets 0, fillx"));
        fvSearchPanel.add(new JLabel("Search:"), "gapright 5");
        fvSearchPanel.add(filterValuesSearchField, "growx");
        filterValuesPreviewPanelContainer.add(fvSearchPanel, BorderLayout.NORTH);
        filterValuesPreviewPanelContainer.add(new JScrollPane(filterValuesPreviewTable), BorderLayout.CENTER);
        filterValuesPreviewTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    createFilterFromSelection(filterExpressionBuilderPanel.getRootGroup());
                }
            }
        });
        previewSplit.setRightComponent(filterValuesPreviewPanelContainer);

        // --- Bottom Area (Results and Actions) ---
        JPanel bottomArea = new JPanel(new BorderLayout(10, 10));
        resultsTabbedPane = new JTabbedPane();
        resultsTabbedPane.setBorder(BorderFactory.createTitledBorder("Result Preview"));
        resultsTabbedPane.setMinimumSize(new Dimension(0, 150));
        consolidatedResultsModel = new DefaultTableModel();
        consolidatedResultsTable = new JTable(consolidatedResultsModel);
        configureTable(consolidatedResultsTable);
        resultsTabbedPane.addTab("Consolidated Results", new JScrollPane(consolidatedResultsTable));
        bottomArea.add(resultsTabbedPane, BorderLayout.CENTER);

        JPanel bottomActionBar = new JPanel(new MigLayout("fillx, align center"));
        bottomActionBar.setBorder(BorderFactory.createEtchedBorder());
        totalMatchesLabel = new JLabel("Total Matches: N/A");
        JButton runFilterButton = new JButton("Run Filter");
        JButton downloadButton = new JButton("Download Results");
        bottomActionBar.add(runFilterButton, "sg actionButton");
        bottomActionBar.add(downloadButton, "sg actionButton");
        bottomActionBar.add(totalMatchesLabel, "gapleft 20");
        bottomArea.add(bottomActionBar, BorderLayout.SOUTH);
        add(bottomArea, BorderLayout.SOUTH);

        runFilterButton.addActionListener(e -> startFilterProcess(ProcessDestination.VIEW));
        downloadButton.addActionListener(e -> startFilterProcess(ProcessDestination.EXPORT));
        configureGroupPanel(filterExpressionBuilderPanel.getRootGroup());
    }

    private void updateFilter(JTextField searchField, TableRowSorter<DefaultTableModel> sorter) {
        String text = searchField.getText();
        if (text.trim().length() == 0) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        }
    }

    private void configureTable(JTable table) {
        table.setFillsViewportHeight(true);
        table.setShowGrid(true);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setDefaultRenderer(Object.class, new TooltipCellRenderer());
    }

    public void configureGroupPanel(LogicalGroupPanel groupPanel) {
        groupPanel.getAddRuleButton().addActionListener(e -> createFilterFromSelection(groupPanel));
        groupPanel.getAddGroupButton().addActionListener(e -> {
            ActionListener deleteListener = event -> {
                LogicalGroupPanel sourceGroup = (LogicalGroupPanel) event.getSource();
                groupPanel.removeComponent(sourceGroup);
            };
            String groupName = com.excelutility.core.AutoNamingService.suggestGroupName();
            LogicalGroupPanel newGroup = new LogicalGroupPanel(groupName, deleteListener, this);
            configureGroupPanel(newGroup);
            groupPanel.addComponent(newGroup);
        });
    }

    private void startFilterProcess(ProcessDestination destination) {
        FilterExpression expression = filterExpressionBuilderPanel.getRootGroup().getExpression();
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

                    if (destination == ProcessDestination.CALCULATE_ONLY) { return; }

                    if (recordCount == 0) {
                        JOptionPane.showMessageDialog(FilterPanel.this, "No records match the filter criteria.", "No Matches", JOptionPane.INFORMATION_MESSAGE);
                        populateTable(consolidatedResultsModel, consolidatedResultsTable, new ArrayList<>());
                        return;
                    }

                    List<String> allColumns = dataFilePanel.getColumnNames();
                    ColumnSelectionDialog colDialog = new ColumnSelectionDialog((Frame) SwingUtilities.getWindowAncestor(FilterPanel.this), allColumns);
                    colDialog.setVisible(true);

                    if (colDialog.isCancelled()) { return; }
                    List<String> selectedColumns = colDialog.getSelectedColumns();
                    List<List<Object>> resultData = projectColumns(filteredData, selectedColumns);

                    if (destination == ProcessDestination.VIEW) {
                        populateTable(consolidatedResultsModel, consolidatedResultsTable, resultData);
                        resultsTabbedPane.setSelectedIndex(0);
                    } else if (destination == ProcessDestination.EXPORT) {
                        promptAndSaveResults(resultData);
                    }
                } catch (Exception e) {
                    totalMatchesLabel.setText("Total Matches: Error");
                    JOptionPane.showMessageDialog(FilterPanel.this, "Failed to apply filters: " + e.getMessage(), "Filtering Error", JOptionPane.ERROR_MESSAGE);
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
                SimpleExcelWriter.writeFilteredResults(filePath, results, true, java.awt.Color.YELLOW);
                int recordCount = filteredData.isEmpty() ? 0 : filteredData.size() - 1;
                JOptionPane.showMessageDialog(this, "Results exported successfully! " + recordCount + " records saved.", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to export results: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
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
        FilterProfile profile = new FilterProfile(profileName.trim(), expression, dataFilePanel.getFilePath(), filterValuesFilePanel.getFilePath(), dataFilePanel.getSelectedSheet(), filterValuesFilePanel.getSelectedSheet());

        try {
            profileService.saveProfile(profile);
            JOptionPane.showMessageDialog(this, "Profile '" + profileName + "' saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving profile: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadProfile() {
        List<String> profiles = profileService.getAvailableProfiles();
        if (profiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No saved filter profiles found.", "Load Profile", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String selectedProfile = (String) JOptionPane.showInputDialog(this, "Select a profile to load:", "Load Filter Profile", JOptionPane.QUESTION_MESSAGE, null, profiles.toArray(), profiles.get(0));
        if (selectedProfile != null) {
            loadProfileByName(selectedProfile);
        }
    }

    private void loadProfileByName(String profileName) {
        try {
            FilterProfile loadedProfile = profileService.loadProfile(profileName);
            rebuildUIFromProfile(loadedProfile);
            JOptionPane.showMessageDialog(this, "Profile '" + profileName + "' loaded successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading profile: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rebuildUIFromProfile(FilterProfile profile) {
        filterExpressionBuilderPanel.getRootGroup().removeAll();
        dataFilePanel.setFilePath(profile.getDataFilePath());
        filterValuesFilePanel.setFilePath(profile.getFilterValuesFilePath());
        dataFilePanel.getSheetCombo().setSelectedItem(profile.getDataFileSheet());
        filterValuesFilePanel.getSheetCombo().setSelectedItem(profile.getFilterValuesFileSheet());
        populateGroupFromNode(filterExpressionBuilderPanel.getRootGroup(), (GroupNode) profile.getRootExpression());
        filterExpressionBuilderPanel.revalidate();
        filterExpressionBuilderPanel.repaint();
    }

    private void populateGroupFromNode(LogicalGroupPanel uiGroup, GroupNode dataNode) {
        uiGroup.setGroupName(dataNode.getName());
        for (FilterExpression childNode : dataNode.getChildren()) {
            if (childNode instanceof com.excelutility.core.expression.RuleNode) {
                com.excelutility.core.expression.RuleNode ruleNode = (com.excelutility.core.expression.RuleNode) childNode;
                filterExpressionBuilderPanel.addRuleToGroup(uiGroup, ruleNode.getRule());
            } else if (childNode instanceof GroupNode) {
                GroupNode childGroupNode = (GroupNode) childNode;
                ActionListener deleteListener = event -> uiGroup.removeComponent((Component) event.getSource());
                LogicalGroupPanel newUiGroup = new LogicalGroupPanel(childGroupNode.getName(), deleteListener, this);
                configureGroupPanel(newUiGroup);
                uiGroup.addComponent(newUiGroup);
                populateGroupFromNode(newUiGroup, childGroupNode);
            }
        }
    }

    private void manageProfiles() {
        ProfileManagerDialog dialog = new ProfileManagerDialog((Frame) SwingUtilities.getWindowAncestor(this), profileService);
        dialog.setVisible(true);
        String profileToLoad = dialog.getSelectedProfileForLoad();
        if (profileToLoad != null) {
            loadProfileByName(profileToLoad);
        }
    }

    public void viewResultsForRule(FilterRule rule) {
        System.out.println("viewResultsForRule called for: " + rule);
    }

    public void downloadResultsForRule(FilterRule rule) {
        System.out.println("downloadResultsForRule called for: " + rule);
    }

    public void calculateCountForRule(FilterRule rule, FilterRulePanel rulePanel) {
        FilterExpression expression = new com.excelutility.core.expression.RuleNode(rule);
        String dataFilePath = dataFilePanel.getFilePath();
        String sheetName = dataFilePanel.getSelectedSheet();

        if (dataFilePath == null || dataFilePath.trim().isEmpty() || sheetName == null) {
            rulePanel.updateCount(-1);
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
                return filteredData.isEmpty() ? 0 : filteredData.size() - 1;
            }

            @Override
            protected void done() {
                try {
                    Integer count = get();
                    rulePanel.updateCount(count);
                } catch (Exception e) {
                    rulePanel.updateCount(-1);
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
        if (targetColumns == null || targetColumns.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Could not retrieve column names from the data file. Please ensure it is loaded correctly.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (int row : selectedRows) {
            for (int col : selectedCols) {
                Object cellValueObj = filterValuesPreviewTable.getValueAt(row, col);
                String cellValue = (cellValueObj == null) ? "" : cellValueObj.toString();
                String columnName = filterValuesPreviewTable.getColumnName(col);

                FilterSourceDialog sourceDialog = new FilterSourceDialog((Frame) SwingUtilities.getWindowAncestor(this), cellValue, columnName);
                sourceDialog.setVisible(true);

                FilterRule.SourceType sourceType = sourceDialog.getSelectedType();
                if (sourceType == null) {
                    continue;
                }
                String sourceValue = sourceDialog.getSelectedValue();

                String dialogTitle = String.format("Step 2/2: Select Target for '%s'", sourceValue);
                FilterTargetDialog targetDialog = new FilterTargetDialog((Frame) SwingUtilities.getWindowAncestor(this), targetColumns, dialogTitle);
                targetDialog.setVisible(true);

                if (targetDialog.isCancelled()) {
                    continue;
                }
                List<String> selectedTargets = targetDialog.getSelectedColumns();
                boolean trim = targetDialog.isTrimWhitespaceSelected();

                for (String target : selectedTargets) {
                    FilterRule rule = new FilterRule(sourceType, sourceValue, target, trim);
                    filterExpressionBuilderPanel.addRuleToGroup(targetGroup, rule);
                }
            }
        }
    }
}
