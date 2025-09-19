package com.excelutility.gui;

import com.excelutility.core.FilterProfile;
import com.excelutility.core.FilterRule;
import com.excelutility.core.FilteringService;
import com.excelutility.core.expression.FilterExpression;
import com.excelutility.io.ExcelReader;
import com.excelutility.core.FilterBuilderState;
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
import java.awt.event.ActionEvent;
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
    private final FilterProfileService filterProfileService = new FilterProfileService();
    boolean isDirty = false;

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
        JButton saveProfileButton = new JButton("Save Profile...");
        saveProfileButton.setToolTipText("Save the current filter configuration (Ctrl+S)");
        JButton loadProfileButton = new JButton("Load Profile...");
        loadProfileButton.setToolTipText("Load a filter configuration from a file (Ctrl+L)");
        JButton addGroupButton = new JButton("Add Group");
        JButton calculateButton = new JButton("Calculate Total");
        JButton viewButton = new JButton("View Overall Result");
        JButton downloadButton = new JButton("Download Filtered Results");
        JButton colorButton = new JButton("Set Highlight Color");
        totalMatchesLabel = new JLabel("Total Matches: N/A");
        actionPanel.add(loadProfileButton);
        actionPanel.add(saveProfileButton);
        actionPanel.add(addGroupButton, "gaptop 10");
        actionPanel.add(calculateButton, "gaptop 10");
        actionPanel.add(viewButton);
        actionPanel.add(totalMatchesLabel, "gaptop 5");
        actionPanel.add(downloadButton, "gaptop 10");
        actionPanel.add(colorButton);

        JButton exitButton = new JButton("Exit");
        actionPanel.add(exitButton, "gaptop 20, align right");

        mainContentPanel.add(actionPanel, "grow");


        // --- Action Listeners ---
        previewButton.addActionListener(e -> loadPreviews());
        colorButton.addActionListener(e -> chooseColor());
        saveProfileButton.addActionListener(e -> saveFilterProfile());
        loadProfileButton.addActionListener(e -> loadFilterProfile());
        downloadButton.addActionListener(e -> startMultiSheetExportProcess());
        viewButton.addActionListener(e -> startFilterProcess(ProcessDestination.VIEW));
        calculateButton.addActionListener(e -> startFilterProcess(ProcessDestination.CALCULATE_ONLY));
        exitButton.addActionListener(e -> exitApplication());

        addGroupButton.addActionListener(e -> {
            LogicalGroupPanel rootGroup = filterExpressionBuilderPanel.getRootGroup();
            ActionListener deleteListener = event -> {
                LogicalGroupPanel sourceGroup = (LogicalGroupPanel) event.getSource();
                rootGroup.removeComponent(sourceGroup);
                updateFilterResults();
                isDirty = true;
            };
            String groupName = com.excelutility.core.AutoNamingService.suggestGroupName();
            LogicalGroupPanel newGroup = new LogicalGroupPanel(groupName, deleteListener, ev -> updateFilterResults());
            newGroup.getAddRuleButton().addActionListener(ev -> createFilterFromSelection(newGroup));
            rootGroup.addComponent(newGroup);
            isDirty = true;
        });

        filterExpressionBuilderPanel.getRootGroup().getAddRuleButton().addActionListener(ev -> createFilterFromSelection(filterExpressionBuilderPanel.getRootGroup()));

        filterValuesPreviewTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    createFilterFromSelection(filterExpressionBuilderPanel.getRootGroup());
                }
            }
        });

        setupKeyboardShortcuts();
    }

    private void setupKeyboardShortcuts() {
        InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = this.getActionMap();

        // Ctrl+S or Cmd+S for Save
        KeyStroke saveKeyStroke = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        inputMap.put(saveKeyStroke, "saveAction");
        actionMap.put("saveAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFilterProfile();
            }
        });

        // Ctrl+L or Cmd+L for Load
        KeyStroke loadKeyStroke = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        inputMap.put(loadKeyStroke, "loadAction");
        actionMap.put("loadAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadFilterProfile();
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

    void updateFilterResults() {
        startFilterProcess(ProcessDestination.VIEW);
        isDirty = true;
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
                // This method is now only used for VIEW and CALCULATE, which apply to the whole expression.
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
                        return; // Stop here for calculations
                    }

                    // For VIEW, we update the main preview tab.
                    if (destination == ProcessDestination.VIEW) {
                        List<String> allColumns = dataFilePanel.getColumnNames();
                        updateUnifiedViewTab(filteredData, allColumns);
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
        runPreviewFilter(rulePanel.getExpression(), rulePanel::setRecordCount, rulePanel.getRuleName() + " Preview");
    }

    public void previewGroup(LogicalGroupPanel groupPanel) {
        runPreviewFilter(groupPanel.getExpression(), groupPanel::setRecordCount, groupPanel.getName() + " Preview");
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
                    if (recordCount > 0) {
                        addPreviewTab(tabTitle, resultData);
                    } else {
                        JOptionPane.showMessageDialog(FilterPanel.this, "No records match the preview criteria.", "No Matches", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    logger.error("Preview filtering process failed.", e);
                    countConsumer.accept(-1);
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
        closeButton.addActionListener(e -> unifiedDataViewTabs.remove(contentPanel));
    }

    private void startMultiSheetExportProcess() {
        String dataFilePath = dataFilePanel.getFilePath();
        String sheetName = dataFilePanel.getSelectedSheet();
        if (dataFilePath == null || dataFilePath.trim().isEmpty() || sheetName == null) {
            JOptionPane.showMessageDialog(this, "Please select a data file and sheet first.", "Data File Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 1. Gather all expressions from the UI
        Map<String, FilterExpression> expressions = new java.util.LinkedHashMap<>();
        for (LogicalGroupPanel groupPanel : filterExpressionBuilderPanel.getRootGroup().getAllGroupPanels()) {
            expressions.put(groupPanel.getName(), groupPanel.getExpression());
        }

        if (expressions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No filter groups to export.", "Export Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2. Prompt user for save location
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Multi-Sheet Export");
        chooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = chooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".xlsx")) {
                filePath += ".xlsx";
            }
            final String finalFilePath = filePath;

            // 3. Run filtering and writing in a background thread
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    // 4. Call the filtering service
                    Map<String, List<List<Object>>> filteredData = filteringService.filterMultiple(
                            dataFilePath,
                            sheetName,
                            dataFilePanel.getHeaderRowIndices(),
                            dataFilePanel.getConcatenationMode(),
                            expressions
                    );

                    // 5. Call the writer
                    SimpleExcelWriter.writeFilteredResults(finalFilePath, filteredData, true, selectedColor);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get(); // Check for exceptions
                        JOptionPane.showMessageDialog(FilterPanel.this, "Multi-sheet export completed successfully!", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        logger.error("Multi-sheet export failed.", e);
                        JOptionPane.showMessageDialog(FilterPanel.this, "Failed to export results: " + e.getCause().getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
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
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        return menuBar;
    }

    private void saveFilterProfile() {
        String profileName = JOptionPane.showInputDialog(this, "Enter a name for this profile:", "Save Profile", JOptionPane.PLAIN_MESSAGE);
        if (profileName == null || profileName.trim().isEmpty()) {
            return;
        }

        try {
            FilterProfile profile = createProfileFromUI(profileName);
            filterProfileService.saveProfile(profile);
            isDirty = false;
            JOptionPane.showMessageDialog(this, "Profile '" + profileName + "' saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            logger.error("Failed to save profile", e);
            JOptionPane.showMessageDialog(this, "Error saving profile: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadFilterProfile() {
        List<File> profiles = filterProfileService.getAvailableProfiles();
        if (profiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No profiles found.", "Load Profile", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        ProfileChooserDialog dialog = new ProfileChooserDialog((Frame) SwingUtilities.getWindowAncestor(this), profiles);
        dialog.setVisible(true);

        File selectedProfileFile = dialog.getSelectedProfile();
        if (selectedProfileFile == null) {
            return; // User cancelled
        }

        if (dialog.isDeleteRequested()) {
            handleProfileDeletion(selectedProfileFile);
        } else {
            handleProfileLoad(selectedProfileFile);
        }
    }

    private void handleProfileLoad(File profileFile) {
        try {
            FilterProfile profile = filterProfileService.loadProfile(profileFile);
            rebuildUIFromProfile(profile);
            isDirty = false;
            JOptionPane.showMessageDialog(this, "Profile '" + profile.getProfileName() + "' loaded successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            logger.error("Failed to load profile", e);
            JOptionPane.showMessageDialog(this, "Error loading profile: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleProfileDeletion(File profileFile) {
        String profileName = profileFile.getName();
        int response = JOptionPane.showConfirmDialog(this,
                "This will permanently remove the profile file:\n" + profileName + "\nAre you sure?",
                "Delete profile \"" + profileName + "\"?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (response == JOptionPane.YES_OPTION) {
            try {
                filterProfileService.deleteProfile(profileFile);
                JOptionPane.showMessageDialog(this, "Profile '" + profileName + "' deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                logger.error("Failed to delete profile", e);
                JOptionPane.showMessageDialog(this, "Error deleting profile: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private FilterProfile createProfileFromUI(String profileName) {
        FilterBuilderState builderState = filterExpressionBuilderPanel.getState();

        return new FilterProfile(
            profileName,
            new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new java.util.Date()),
            dataFilePanel.getFilePath(),
            dataFilePanel.getSelectedSheet(),
            filterValuesFilePanel.getFilePath(),
            filterValuesFilePanel.getSelectedSheet(),
            builderState
        );
    }

    private void rebuildUIFromProfile(FilterProfile profile) {
        dataFilePanel.setFileAndSheet(profile.getDataFilePath(), profile.getDataSheet());
        filterValuesFilePanel.setFileAndSheet(profile.getFilterFilePath(), profile.getFilterSheet());

        filterExpressionBuilderPanel.rebuildFromState(profile.getFilterBuilder());

        // Load previews for the newly set files and update the results based on the new filters
        loadPreviews();
        updateFilterResults();

        logger.info("Profile '{}' loaded.", profile.getProfileName());
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
                LogicalGroupPanel newUiGroup = new LogicalGroupPanel(childGroupNode.getName(), deleteListener, ev -> updateFilterResults());
                newUiGroup.getAddRuleButton().addActionListener(e -> createFilterFromSelection(newUiGroup));
                uiGroup.addComponent(newUiGroup);
                populateGroupFromNode(newUiGroup, childGroupNode);
            }
        }
    }

    private void manageProfiles() {
        JOptionPane.showMessageDialog(this, "Profile Manager is not yet implemented.", "Not Implemented", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exitApplication() {
        if (isDirty) {
            String[] options = {"Save & Exit", "Exit without Saving", "Cancel"};
            int response = JOptionPane.showOptionDialog(this,
                    "You have unsaved changes to your profile. Do you want to Save before exiting?",
                    "Exit Application",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);

            switch (response) {
                case 0: // Save & Exit
                    saveFilterProfile();
                    // If save was successful, isDirty will be false. We can exit.
                    // If save was cancelled by user, isDirty will still be true. We don't exit.
                    if (!isDirty) {
                        System.exit(0);
                    }
                    break;
                case 1: // Exit without Saving
                    System.exit(0);
                    break;
                case 2: // Cancel
                default:
                    // Do nothing
                    break;
            }
        } else {
            System.exit(0);
        }
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
