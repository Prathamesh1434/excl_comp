package com.excelutility.gui;

import com.excelutility.core.ComparisonProfile;
import com.excelutility.core.ComparisonResult;
import com.excelutility.core.ComparisonService;
import com.excelutility.core.KeySuggester;
import com.excelutility.io.ExcelReader;
import com.excelutility.io.ProfileService;
import com.excelutility.io.SimpleExcelWriter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

public class MainFrame extends JFrame {

    private final ComparisonProfile profile = new ComparisonProfile();
    private final ComparisonService comparisonService = new ComparisonService();
    private final ProfileService profileService = new ProfileService("profiles");

    private final FileConfigPanel sourceFilePanel;
    private final FileConfigPanel targetFilePanel;
    private final ColumnMappingPanel columnMappingPanel;
    private final JTable resultsTable;
    private final ResultTableModel resultsTableModel;
    private final JLabel statusLabel;

    private final DefaultTableModel sourcePreviewModel;
    private final DefaultTableModel targetPreviewModel;

    private List<String> sourceHeaders;
    private List<String> targetHeaders;

    public MainFrame() {
        setTitle("Excel Utility");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1600, 1000);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        // --- Menu Bar ---
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem saveProfileItem = new JMenuItem("Save Profile As...");
        saveProfileItem.addActionListener(e -> saveProfile());
        fileMenu.add(saveProfileItem);

        JMenuItem loadProfileItem = new JMenuItem("Load Profile...");
        loadProfileItem.addActionListener(e -> loadProfile());
        fileMenu.add(loadProfileItem);

        JMenuItem exportResultsItem = new JMenuItem("Export Results...");
        exportResultsItem.addActionListener(e -> exportResults());
        fileMenu.add(exportResultsItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        JMenu editMenu = new JMenu("Edit");
        JMenuItem profileManagerItem = new JMenuItem("Profile Manager...");
        profileManagerItem.addActionListener(e -> openProfileManager());
        editMenu.add(profileManagerItem);
        menuBar.add(editMenu);

        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem runMenuItem = new JMenuItem("Run Comparison");
        runMenuItem.addActionListener(e -> runComparison());
        toolsMenu.add(runMenuItem);
        JMenuItem testGenItem = new JMenuItem("Generate Test Cases...");
        testGenItem.addActionListener(e -> openTestCaseGenerator());
        toolsMenu.add(testGenItem);
        menuBar.add(toolsMenu);

        setJMenuBar(menuBar);

        // --- Main Panel ---
        JPanel mainPanel = new JPanel(new MigLayout("fill", "[grow]", "[][grow]"));

        // --- Top Control Panel ---
        JPanel topPanel = new JPanel(new MigLayout("fillx", "[grow][grow]"));
        sourceFilePanel = new FileConfigPanel("Source File", this);
        targetFilePanel = new FileConfigPanel("Target File", this);
        topPanel.add(sourceFilePanel, "growx");
        topPanel.add(targetFilePanel, "growx");
        mainPanel.add(topPanel, "dock north");

        // --- Center Split Pane ---
        JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        centerSplit.setResizeWeight(0.5);

        // --- Top part of Center Split: Mappings and Previews ---
        columnMappingPanel = new ColumnMappingPanel();

        // Preview Panels
        sourcePreviewModel = new DefaultTableModel();
        JTable sourcePreviewTable = new JTable(sourcePreviewModel);
        JScrollPane sourcePreviewScroll = new JScrollPane(sourcePreviewTable);
        sourcePreviewScroll.setBorder(BorderFactory.createTitledBorder("Source Preview"));

        targetPreviewModel = new DefaultTableModel();
        JTable targetPreviewTable = new JTable(targetPreviewModel);
        JScrollPane targetPreviewScroll = new JScrollPane(targetPreviewTable);
        targetPreviewScroll.setBorder(BorderFactory.createTitledBorder("Target Preview"));

        JSplitPane previewSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sourcePreviewScroll, targetPreviewScroll);
        previewSplit.setResizeWeight(0.5);

        JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, columnMappingPanel, previewSplit);
        topSplit.setResizeWeight(0.5);

        // --- Bottom part of Center Split: Results Table ---
        resultsTableModel = new ResultTableModel();
        resultsTable = new JTable(resultsTableModel);
        resultsTable.setDefaultRenderer(Object.class, new ResultTableRenderer());
        JPanel resultsPanel = new JPanel(new MigLayout("fill", "[grow]", "[grow]"));
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Comparison Results"));
        resultsPanel.add(new JScrollPane(resultsTable), "grow");

        centerSplit.setTopComponent(topSplit);
        centerSplit.setBottomComponent(resultsPanel);

        mainPanel.add(centerSplit, "grow");
        add(mainPanel, BorderLayout.CENTER);

        // --- Status Bar ---
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Ready.");
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.SOUTH);

        // --- Action Listeners ---
        sourceFilePanel.getAutoSuggestButton().addActionListener(e -> autoSuggestKeys());
        sourceFilePanel.getFilterButton().addActionListener(e -> openFilterBuilder(true));
        targetFilePanel.getFilterButton().addActionListener(e -> openFilterBuilder(false));

        sourceFilePanel.getPreviewButton().addActionListener(e -> showPreview(true));
        targetFilePanel.getPreviewButton().addActionListener(e -> showPreview(false));

        // Listener to reload headers when a new sheet is selected
        sourceFilePanel.getSheetCombo().addActionListener(e -> {
            if (e.getActionCommand().equals("comboBoxChanged") && sourceFilePanel.getSheetCombo().getSelectedItem() != null) {
                loadHeaders(true);
            }
        });
        targetFilePanel.getSheetCombo().addActionListener(e -> {
            if (e.getActionCommand().equals("comboBoxChanged") && targetFilePanel.getSheetCombo().getSelectedItem() != null) {
                loadHeaders(false);
            }
        });
    }

    private void loadHeaders(boolean isSource) {
        FileConfigPanel panel = isSource ? sourceFilePanel : targetFilePanel;
        String filePath = panel.getFilePath();
        String sheetName = panel.getSelectedSheet();

        if (filePath == null || filePath.trim().isEmpty() || sheetName == null) {
            return; // Not ready to load headers
        }

        try {
            List<List<Object>> headerData = ExcelReader.read(filePath, sheetName, true); // Assuming read gives all data for now
            if (headerData.isEmpty()) {
                if (isSource) this.sourceHeaders = List.of(); else this.targetHeaders = List.of();
            } else {
                List<String> headers = headerData.get(0).stream().map(Object::toString).collect(Collectors.toList());
                if (isSource) this.sourceHeaders = headers; else this.targetHeaders = headers;
            }

            if (this.sourceHeaders != null && this.targetHeaders != null) {
                columnMappingPanel.setColumns(this.sourceHeaders, this.targetHeaders);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error reading header data from file: " + filePath, "Header Read Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runComparison() {
        if (sourceFilePanel.getSelectedSheet() == null || targetFilePanel.getSelectedSheet() == null) {
            JOptionPane.showMessageDialog(this, "Please select a sheet for both files.", "Sheet Not Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        profile.setSourceFilePath(sourceFilePanel.getFilePath());
        profile.setSourceSheetName(sourceFilePanel.getSelectedSheet());
        profile.setTargetFilePath(targetFilePanel.getFilePath());
        profile.setTargetSheetName(targetFilePanel.getSelectedSheet());

        Map<String, String> mappings = columnMappingPanel.getColumnMappings();
        List<String> keyColumns = columnMappingPanel.getKeyColumns();

        if (keyColumns.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select at least one 'Is Key' column for row matching.", "No Key Columns Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        profile.setColumnMappings(mappings);
        profile.setKeyColumns(keyColumns);

        statusLabel.setText("Running comparison...");

        new SwingWorker<ComparisonResult, Void>() {
            @Override
            protected ComparisonResult doInBackground() throws Exception {
                return comparisonService.compare(profile);
            }

            @Override
            protected void done() {
                try {
                    ComparisonResult result = get();
                    resultsTableModel.setComparisonResult(result);
                    statusLabel.setText("Comparison complete. " + result.getStats().matchedMismatched + " mismatches found.");
                } catch (Exception e) {
                    statusLabel.setText("Error during comparison.");
                    JOptionPane.showMessageDialog(MainFrame.this, "An error occurred: " + e.getMessage(), "Comparison Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    private void showPreview(boolean isSource) {
        FileConfigPanel panel = isSource ? sourceFilePanel : targetFilePanel;
        String filePath = panel.getFilePath();
        String sheetName = panel.getSelectedSheet();

        if (filePath == null || filePath.trim().isEmpty() || sheetName == null) {
            JOptionPane.showMessageDialog(this, "Please select a file and a sheet to preview.", "Cannot Preview", JOptionPane.WARNING_MESSAGE);
            return;
        }

        statusLabel.setText("Loading preview...");

        new SwingWorker<List<List<Object>>, Void>() {
            @Override
            protected List<List<Object>> doInBackground() throws Exception {
                return ExcelReader.readPreview(filePath, sheetName, 10);
            }

            @Override
            protected void done() {
                try {
                    List<List<Object>> previewData = get();
                    DefaultTableModel model = isSource ? sourcePreviewModel : targetPreviewModel;

                    if (previewData == null || previewData.isEmpty()) {
                        model.setDataVector(new Vector<>(), new Vector<>());
                        statusLabel.setText("Preview loaded. No data found.");
                        return;
                    }

                    // First row is the header
                    Vector<String> headers = new Vector<>();
                    for (Object header : previewData.get(0)) {
                        headers.add(header != null ? header.toString() : "");
                    }

                    // The rest of the rows are data
                    Vector<Vector<Object>> data = new Vector<>();
                    if (previewData.size() > 1) {
                        for (int i = 1; i < previewData.size(); i++) {
                            data.add(new Vector<>(previewData.get(i)));
                        }
                    }

                    model.setDataVector(data, headers);
                    statusLabel.setText("Preview loaded successfully.");

                } catch (Exception e) {
                    statusLabel.setText("Error loading preview.");
                    JOptionPane.showMessageDialog(MainFrame.this, "Could not load preview: " + e.getMessage(), "Preview Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void openTestCaseGenerator() {
        TestCaseGeneratorDialog dialog = new TestCaseGeneratorDialog(this);
        dialog.setVisible(true);
    }

    private void openProfileManager() {
        ProfileManagerDialog dialog = new ProfileManagerDialog(this, profileService);
        dialog.setVisible(true);
    }

    private void saveProfile() {
        String profileName = JOptionPane.showInputDialog(this, "Enter a name for this profile:", "Save Profile", JOptionPane.PLAIN_MESSAGE);
        if (profileName != null && !profileName.trim().isEmpty()) {
            try {
                updateProfileFromGui();
                profileService.saveProfile(profile, profileName);
                JOptionPane.showMessageDialog(this, "Profile saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving profile: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadProfile() {
        List<String> profiles = profileService.getAvailableProfiles();
        if (profiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No saved profiles found.", "Load Profile", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String selectedProfile = (String) JOptionPane.showInputDialog(this, "Select a profile to load:",
                "Load Profile", JOptionPane.QUESTION_MESSAGE, null, profiles.toArray(), profiles.get(0));

        if (selectedProfile != null) {
            try {
                ComparisonProfile loadedProfile = profileService.loadProfile(selectedProfile);
                updateGuiFromProfile(loadedProfile);
                JOptionPane.showMessageDialog(this, "Profile '" + selectedProfile + "' loaded successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error loading profile: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateProfileFromGui() {
        profile.setSourceFilePath(sourceFilePanel.getFilePath());
        profile.setSourceSheetName(sourceFilePanel.getSelectedSheet());
        profile.setTargetFilePath(targetFilePanel.getFilePath());
        profile.setTargetSheetName(targetFilePanel.getSelectedSheet());
        profile.setColumnMappings(columnMappingPanel.getColumnMappings());
        profile.setKeyColumns(columnMappingPanel.getKeyColumns());
    }

    private void updateGuiFromProfile(ComparisonProfile loadedProfile) {
        sourceFilePanel.setFilePath(loadedProfile.getSourceFilePath());
        targetFilePanel.setFilePath(loadedProfile.getTargetFilePath());

        // The setFilePath method populates the sheet combo. We just need to set the selected item.
        // A short delay might be needed if sheet loading is slow, but we'll try without first.
        SwingUtilities.invokeLater(() -> {
            sourceFilePanel.setSelectedSheet(loadedProfile.getSourceSheetName());
            targetFilePanel.setSelectedSheet(loadedProfile.getTargetSheetName());

            // Manually trigger header loading after profile is loaded
            if (loadedProfile.getSourceSheetName() != null) {
                loadHeaders(true);
            }
            if (loadedProfile.getTargetSheetName() != null) {
                loadHeaders(false);
            }

            // Update the column mapping panel with the loaded settings
            if (this.sourceHeaders != null && this.targetHeaders != null &&
                loadedProfile.getColumnMappings() != null && loadedProfile.getKeyColumns() != null) {
                columnMappingPanel.setMappings(loadedProfile.getColumnMappings(), loadedProfile.getKeyColumns());
            }
        });
    }

    private void autoSuggestKeys() {
        String sourcePath = sourceFilePanel.getFilePath();
        String sourceSheet = sourceFilePanel.getSelectedSheet();

        if (sourcePath == null || sourcePath.trim().isEmpty() || sourceSheet == null) {
            JOptionPane.showMessageDialog(this, "Please load a source file and select a sheet first.", "Source File Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            statusLabel.setText("Analyzing source file for key suggestions...");
            List<List<Object>> data = ExcelReader.read(sourcePath, sourceSheet, true);
            if (data.size() < 2) { // Need header + at least one data row
                statusLabel.setText("Not enough data to suggest keys.");
                return;
            }
            List<Object> headers = data.remove(0);

            List<KeySuggester.KeySuggestion> suggestions = KeySuggester.suggestKeys(data, headers);

            KeySuggestionDialog dialog = new KeySuggestionDialog(this, suggestions);
            dialog.setVisible(true);

            if (dialog.isAccepted()) {
                List<String> selectedKeys = dialog.getSelectedSuggestions().stream()
                        .map(KeySuggester.KeySuggestion::getColumnName)
                        .collect(Collectors.toList());
                columnMappingPanel.selectKeys(selectedKeys);
            }
            statusLabel.setText("Ready.");
        } catch (Exception e) {
            statusLabel.setText("Error during key suggestion.");
            JOptionPane.showMessageDialog(this, "Error suggesting keys: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void openFilterBuilder(boolean isSource) {
        List<String> columns = isSource ? sourceHeaders : targetHeaders;
        if (columns == null || columns.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please load a file and select a sheet first.", "No Columns Found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        FilterBuilderDialog dialog = new FilterBuilderDialog(this, columns);
        dialog.setVisible(true);
    }

    private void exportResults() {
        if (resultsTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "There are no results to export.", "Export Results", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Exported Results");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = chooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".xlsx")) {
                filePath += ".xlsx";
            }

            final String finalFilePath = filePath;
            statusLabel.setText("Exporting results to Excel...");

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    ComparisonResult result = resultsTableModel.getComparisonResult();
                    SimpleExcelWriter.writeComparisonResult(result, finalFilePath);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get(); // Check for exceptions
                        statusLabel.setText("Results exported successfully.");
                        JOptionPane.showMessageDialog(MainFrame.this, "Results exported successfully to:\n" + finalFilePath, "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        statusLabel.setText("Error during export.");
                        JOptionPane.showMessageDialog(MainFrame.this, "Failed to export results: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                }
            }.execute();
        }
    }
}
