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
import java.util.regex.Pattern;

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
    private final JTextField searchField = new JTextField();

    private enum ProcessDestination { VIEW, EXPORT, CALCULATE_ONLY }

    private Color selectedColor = new Color(255, 255, 153); // A lighter yellow
    private final AppContainer appContainer;
    private final FilterProfileService profileService = new FilterProfileService();

    public FilterPanel(AppContainer appContainer) {
        this.appContainer = appContainer;
        setLayout(new MigLayout("fill, insets 15", "[50%, grow, fill][50%, grow, fill]", "[][grow, fill][]"));

        // --- File Panels ---
        dataFilePanel = new FilterFilePanel("Data File (to be filtered)", this);
        filterValuesFilePanel = new FilterFilePanel("Filter Values File", this);
        add(dataFilePanel, "grow");
        add(filterValuesFilePanel, "grow, wrap");

        // --- Main Content Area ---
        JPanel mainContentPanel = new JPanel(new MigLayout("fill, insets 0", "[grow, fill]", "[][grow, fill]"));

        // --- Preview Tables ---
        dataPreviewModel = new DefaultTableModel();
        dataPreviewTable = new JTable(dataPreviewModel);
        configureTable(dataPreviewTable);
        JScrollPane dataPreviewScroll = new JScrollPane(dataPreviewTable);
        mainContentPanel.add(createTitledPanel("Data Preview (First 50 rows)", dataPreviewScroll), "grow, h 50%, wrap");

        filterValuesPreviewModel = new DefaultTableModel();
        filterValuesPreviewTable = new JTable(filterValuesPreviewModel);
        configureTable(filterValuesPreviewTable);
        filterValuesPreviewTable.setCellSelectionEnabled(true);
        JScrollPane filterValuesPreviewScroll = new JScrollPane(filterValuesPreviewTable);
        mainContentPanel.add(createTitledPanel("Filter Values Preview (Double-click to create a filter)", filterValuesPreviewScroll), "grow, h 50%");

        add(mainContentPanel, "grow");

        // --- Filter Builder and Actions ---
        filterExpressionBuilderPanel = new FilterExpressionBuilderPanel(this);
        add(createTitledPanel("Filter Logic Builder", filterExpressionBuilderPanel), "grow, wrap");

        // --- Bottom Action Bar ---
        JPanel bottomBar = new JPanel(new MigLayout("fillx, insets 5 0 0 0", "[][]push[][]"));
        JButton loadButton = new JButton("Load & Preview Files");
        loadButton.setFont(UIConstants.FONT_BUTTON);
        bottomBar.add(loadButton);

        bottomBar.add(new JLabel("Preview Search:"), "gapleft 20");
        bottomBar.add(searchField, "w 200!");

        JButton calculateButton = new JButton("Calculate Total");
        calculateButton.setFont(UIConstants.FONT_BUTTON);
        bottomBar.add(calculateButton);

        totalMatchesLabel = new JLabel("Total Matches: N/A");
        totalMatchesLabel.setFont(UIConstants.FONT_LABEL);
        bottomBar.add(totalMatchesLabel, "gapleft 10");

        JButton viewButton = new JButton("View Results");
        viewButton.setFont(UIConstants.FONT_BUTTON);
        bottomBar.add(viewButton);

        JButton downloadButton = new JButton("Download Results...");
        downloadButton.setFont(UIConstants.FONT_BUTTON);
        downloadButton.setBackground(UIConstants.COLOR_PRIMARY_BUTTON);
        downloadButton.setForeground(Color.WHITE);
        bottomBar.add(downloadButton);

        add(bottomBar, "span, growx");

        // --- Action Listeners ---
        loadButton.addActionListener(e -> loadPreviews());
        downloadButton.addActionListener(e -> startFilterProcess(ProcessDestination.EXPORT));
        viewButton.addActionListener(e -> startFilterProcess(ProcessDestination.VIEW));
        calculateButton.addActionListener(e -> startFilterProcess(ProcessDestination.CALCULATE_ONLY));

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { searchTables(searchField.getText()); }
            public void removeUpdate(DocumentEvent e) { searchTables(searchField.getText()); }
            public void changedUpdate(DocumentEvent e) { searchTables(searchField.getText()); }
        });

        filterValuesPreviewTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    createFilterFromSelection(filterExpressionBuilderPanel.getRootGroup());
                }
            }
        });
    }

    private JPanel createTitledPanel(String title, JComponent component) {
        JPanel panel = new JPanel(new MigLayout("fill, insets 0", "[grow, fill]", "[][grow, fill]"));
        JLabel label = new JLabel(title);
        label.setFont(UIConstants.FONT_LABEL.deriveFont(Font.BOLD));
        label.setForeground(UIConstants.COLOR_TEXT_BODY);
        panel.add(label, "wrap, gapbottom 5");
        panel.add(component, "grow");
        panel.setBorder(UIConstants.BORDER_PANEL);
        panel.setBackground(getBackground());
        return panel;
    }

    private void searchTables(String text) {
        RowFilter<Object, Object> rf = null;
        if (text != null && !text.trim().isEmpty()) {
            try {
                rf = RowFilter.regexFilter("(?i)" + Pattern.quote(text));
            } catch (java.util.regex.PatternSyntaxException e) {
                return; // Ignore invalid regex
            }
        }
        ((TableRowSorter) dataPreviewTable.getRowSorter()).setRowFilter(rf);
        ((TableRowSorter) filterValuesPreviewTable.getRowSorter()).setRowFilter(rf);
    }

    private void configureTable(JTable table) {
        table.setRowHeight(24);
        table.setShowGrid(true);
        table.setGridColor(new Color(220, 220, 220));
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFont(UIConstants.FONT_BODY);
        table.getTableHeader().setFont(UIConstants.FONT_LABEL.deriveFont(Font.BOLD));
    }

    private void adjustColumnWidths(JTable table) {
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = 150; // Min width
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width + 15, width);
            }
            if (width > 400) width = 400; // Max width
            table.getColumnModel().getColumn(column).setPreferredWidth(width);
        }
    }

    private void startFilterProcess(ProcessDestination destination) {
        FilterExpression expression = filterExpressionBuilderPanel.getRootGroup().getExpression();
        String dataFilePath = dataFilePanel.getFilePath();
        String sheetName = dataFilePanel.getSelectedSheet();
        if (dataFilePath == null || dataFilePath.trim().isEmpty() || sheetName == null) {
            JOptionPane.showMessageDialog(this, "Please select a data file and sheet first.", "Data File Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        totalMatchesLabel.setText("Calculating...");
        new SwingWorker<List<List<Object>>, Void>() {
            protected List<List<Object>> doInBackground() throws Exception {
                return filteringService.filter(dataFilePath, sheetName, dataFilePanel.getHeaderRowIndices(), dataFilePanel.getConcatenationMode(), expression);
            }

            protected void done() {
                try {
                    List<List<Object>> filteredData = get();
                    int recordCount = filteredData.isEmpty() ? 0 : filteredData.size() - 1;
                    totalMatchesLabel.setText("Matches: " + recordCount);

                    if (destination == ProcessDestination.CALCULATE_ONLY) return;
                    if (recordCount == 0) {
                        JOptionPane.showMessageDialog(FilterPanel.this, "No records match the filter criteria.", "No Matches", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    List<String> allColumns = dataFilePanel.getColumnNames();
                    ColumnSelectionDialog colDialog = new ColumnSelectionDialog((Frame) SwingUtilities.getWindowAncestor(FilterPanel.this), allColumns);
                    colDialog.setVisible(true);

                    if (colDialog.isCancelled()) return;
                    List<String> selectedColumns = colDialog.getSelectedColumns();
                    List<List<Object>> resultData = projectColumns(filteredData, selectedColumns);

                    if (destination == ProcessDestination.VIEW) {
                        new ResultsViewerDialog((Frame) SwingUtilities.getWindowAncestor(FilterPanel.this), "Overall Filter Results", resultData).setVisible(true);
                    } else if (destination == ProcessDestination.EXPORT) {
                        promptAndSaveResults(resultData);
                    }
                } catch (Exception e) {
                    logger.error("Filtering process failed.", e);
                    totalMatchesLabel.setText("Error");
                    JOptionPane.showMessageDialog(FilterPanel.this, "Failed to apply filters: " + e.getCause().getMessage(), "Filtering Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void promptAndSaveResults(List<List<Object>> filteredData) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Filtered Results");
        chooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = chooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".xlsx")) filePath += ".xlsx";
            try {
                SimpleExcelWriter.writeFilteredResults(filePath, Map.of("Filtered_Results", filteredData), true, selectedColor);
                JOptionPane.showMessageDialog(this, "Results exported successfully!", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                logger.error("Export failed.", e);
                JOptionPane.showMessageDialog(this, "Failed to export results: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void viewResultsForRule(FilterRule rule) {
        // This logic can be simplified or removed if per-rule actions are not central to the new UI
    }

    public void downloadResultsForRule(FilterRule rule) {
        // This logic can be simplified or removed
    }

    private List<List<Object>> projectColumns(List<List<Object>> data, List<String> columnsToKeep) {
        if (data.isEmpty() || columnsToKeep.isEmpty()) return data;

        List<String> originalHeader = data.get(0).stream().map(Object::toString).collect(Collectors.toList());
        List<Integer> indicesToKeep = columnsToKeep.stream()
            .map(originalHeader::indexOf)
            .filter(i -> i != -1)
            .collect(Collectors.toList());

        return data.stream()
            .map(row -> indicesToKeep.stream().map(row::get).collect(Collectors.toList()))
            .collect(Collectors.toList());
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
            JOptionPane.showMessageDialog(this, "Could not get columns from the data file. Please load it first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (int row : selectedRows) {
            for (int col : selectedCols) {
                Object cellValueObj = filterValuesPreviewTable.getValueAt(row, col);
                String cellValue = (cellValueObj == null) ? "" : cellValueObj.toString();
                String columnName = filterValuesPreviewTable.getColumnName(col);

                FilterSourceDialog sourceDialog = new FilterSourceDialog((Frame) SwingUtilities.getWindowAncestor(this), cellValue, columnName);
                sourceDialog.setVisible(true);
                if (sourceDialog.getSelectedType() == null) continue;

                FilterTargetDialog targetDialog = new FilterTargetDialog((Frame) SwingUtilities.getWindowAncestor(this), targetColumns, "Select Target Column(s)");
                targetDialog.setVisible(true);
                if (targetDialog.isCancelled()) continue;

                for (String target : targetDialog.getSelectedColumns()) {
                    FilterRule rule = new FilterRule(sourceDialog.getSelectedType(), sourceDialog.getSelectedValue(), target, targetDialog.isTrimWhitespaceSelected());
                    filterExpressionBuilderPanel.addRuleToGroup(targetGroup, rule);
                }
            }
        }
    }

    private void loadPreviews() {
        loadTableData(dataFilePanel, dataPreviewModel, 50, "Error loading data preview", dataPreviewTable);
        loadTableData(filterValuesFilePanel, filterValuesPreviewModel, -1, "Error loading filter values", filterValuesPreviewTable);
    }

    private void loadTableData(FilterFilePanel panel, DefaultTableModel model, int rowLimit, String errorTitle, JTable table) {
        String filePath = panel.getFilePath();
        String sheetName = panel.getSelectedSheet();

        if (filePath == null || filePath.trim().isEmpty() || sheetName == null) {
            return; // Fail silently if called before file selection
        }

        new SwingWorker<List<List<Object>>, Exception>() {
            protected List<List<Object>> doInBackground() throws Exception {
                return (rowLimit > 0)
                    ? ExcelReader.readPreview(filePath, sheetName, rowLimit)
                    : ExcelReader.read(filePath, sheetName, true);
            }

            protected void done() {
                try {
                    List<List<Object>> data = get();
                    if (data == null || data.isEmpty()) {
                        model.setDataVector(new Vector<>(), new Vector<>());
                        return;
                    }

                    Vector<String> headerVector = new Vector<>(panel.getColumnNames());
                    Vector<Vector<Object>> dataVector = new Vector<>();
                    int headerRowCount = panel.getHeaderRowIndices().size();

                    if (data.size() > headerRowCount) {
                        List<List<Object>> dataRows = data.subList(headerRowCount, data.size());
                        for(List<Object> row : dataRows) {
                            dataVector.add(new Vector<>(row));
                        }
                    }
                    model.setDataVector(dataVector, headerVector);
                    adjustColumnWidths(table);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(FilterPanel.this, "Could not load data: " + e.getMessage(), errorTitle, JOptionPane.ERROR_MESSAGE);
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

        JMenuItem saveProfileItem = new JMenuItem("Save Filter Profile...");
        saveProfileItem.addActionListener(e -> saveProfile());
        fileMenu.add(saveProfileItem);

        JMenuItem loadProfileItem = new JMenuItem("Load Filter Profile...");
        loadProfileItem.addActionListener(e -> loadProfile());
        fileMenu.add(loadProfileItem);

        fileMenu.addSeparator();
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
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
        String selectedProfile = (String) JOptionPane.showInputDialog(this, "Select a profile to load:", "Load Filter Profile", JOptionPane.QUESTION_MESSAGE, null, profiles.toArray(), profiles.get(0));
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
        // Logic to rebuild UI from a loaded profile
    }
}
