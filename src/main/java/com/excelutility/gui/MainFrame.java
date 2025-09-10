package com.excelutility.gui;

import com.excelutility.core.ComparisonProfile;
import com.excelutility.core.ComparisonResult;
import com.excelutility.core.ComparisonService;
import com.excelutility.core.KeySuggester;
import com.excelutility.io.ExcelReader;
import com.excelutility.io.ProfileService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainFrame extends JFrame {

    private final ComparisonProfile profile = new ComparisonProfile();
    private final ComparisonService comparisonService = new ComparisonService();
    private final ProfileService profileService = new ProfileService("profiles");

    private FileConfigPanel sourceFilePanel;
    private FileConfigPanel targetFilePanel;
    private ColumnMappingPanel columnMappingPanel;
    private JTable resultsTable;
    private ResultTableModel resultsTableModel;
    private JLabel statusLabel;
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

        // File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem openSourceItem = new JMenuItem("Open Source File...");
        openSourceItem.addActionListener(e -> selectFile(true));
        fileMenu.add(openSourceItem);
        JMenuItem openTargetItem = new JMenuItem("Open Target File...");
        openTargetItem.addActionListener(e -> selectFile(false));
        fileMenu.add(openTargetItem);
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

        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        JMenuItem profileManagerItem = new JMenuItem("Profile Manager...");
        profileManagerItem.addActionListener(e -> openProfileManager());
        editMenu.add(profileManagerItem);
        menuBar.add(editMenu);

        // Tools Menu
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
        sourceFilePanel = new FileConfigPanel("Source File");
        targetFilePanel = new FileConfigPanel("Target File");
        sourceFilePanel.getAutoSuggestButton().addActionListener(e -> autoSuggestKeys());
        sourceFilePanel.getFilterButton().addActionListener(e -> openFilterBuilder(true));
        targetFilePanel.getFilterButton().addActionListener(e -> openFilterBuilder(false));
        topPanel.add(sourceFilePanel, "growx");
        topPanel.add(targetFilePanel, "growx");
        mainPanel.add(topPanel, "dock north");

        // --- Center Split Pane ---
        JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        centerSplit.setResizeWeight(0.5);

        // Preview and Options
        columnMappingPanel = new ColumnMappingPanel();

        JPanel topRightPanel = new JPanel(new MigLayout("fill", "[grow]", "[grow]"));
        topRightPanel.setBorder(BorderFactory.createTitledBorder("Normalization & Comparison Rules"));
        topRightPanel.add(new JLabel("Normalization rules will go here."), "grow");

        JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, columnMappingPanel, topRightPanel);
        topSplit.setResizeWeight(0.5);

        // Results Table
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
    }

    private void selectFile(boolean isSource) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Excel Files", "xls", "xlsx"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            FileConfigPanel panel = isSource ? sourceFilePanel : targetFilePanel;
            if (isSource) {
                profile.setSourceFilePath(file.getAbsolutePath());
            } else {
                profile.setTargetFilePath(file.getAbsolutePath());
            }
            panel.getFileField().setText(file.getAbsolutePath());
            populateSheetCombo(panel.getSheetCombo(), file.getAbsolutePath(), isSource);
        }
    }

    private void populateSheetCombo(JComboBox<String> combo, String filePath, boolean isSource) {
        try {
            List<String> sheetNames = ExcelReader.getSheetNames(filePath);
            combo.removeAllItems();
            for (String name : sheetNames) {
                combo.addItem(name);
            }
            if (!sheetNames.isEmpty()) {
                combo.setSelectedIndex(0);
                loadHeaders(filePath, sheetNames.get(0), isSource);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading sheets from file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadHeaders(String filePath, String sheetName, boolean isSource) {
        try {
            List<List<Object>> headerData = ExcelReader.read(filePath, sheetName, true);
            if (headerData.isEmpty()) {
                if (isSource) this.sourceHeaders = new ArrayList<>(); else this.targetHeaders = new ArrayList<>();
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
        if (sourceFilePanel.getSheetCombo().getSelectedItem() == null || targetFilePanel.getSheetCombo().getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Please select a sheet for both files.", "Sheet Not Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        profile.setSourceSheetName(sourceFilePanel.getSheetCombo().getSelectedItem().toString());
        profile.setTargetSheetName(targetFilePanel.getSheetCombo().getSelectedItem().toString());

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
        profile.setSourceSheetName(sourceFilePanel.getSheetCombo().getSelectedItem() != null ? sourceFilePanel.getSheetCombo().getSelectedItem().toString() : null);
        profile.setTargetSheetName(targetFilePanel.getSheetCombo().getSelectedItem() != null ? targetFilePanel.getSheetCombo().getSelectedItem().toString() : null);
        profile.setColumnMappings(columnMappingPanel.getColumnMappings());
        profile.setKeyColumns(columnMappingPanel.getKeyColumns());
    }

    private void updateGuiFromProfile(ComparisonProfile loadedProfile) {
        sourceFilePanel.getFileField().setText(loadedProfile.getSourceFilePath());
        targetFilePanel.getFileField().setText(loadedProfile.getTargetFilePath());
        profile.setSourceFilePath(loadedProfile.getSourceFilePath());
        profile.setTargetFilePath(loadedProfile.getTargetFilePath());

        populateSheetCombo(sourceFilePanel.getSheetCombo(), loadedProfile.getSourceFilePath(), true);
        sourceFilePanel.getSheetCombo().setSelectedItem(loadedProfile.getSourceSheetName());

        populateSheetCombo(targetFilePanel.getSheetCombo(), loadedProfile.getTargetFilePath(), false);
        targetFilePanel.getSheetCombo().setSelectedItem(loadedProfile.getTargetSheetName());
    }

    private void autoSuggestKeys() {
        if (profile.getSourceFilePath() == null) {
            JOptionPane.showMessageDialog(this, "Please load a source file first.", "Source File Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            statusLabel.setText("Analyzing source file for key suggestions...");
            List<List<Object>> data = ExcelReader.read(profile.getSourceFilePath(), profile.getSourceSheetName(), true);
            List<Object> headers = data.remove(0);

            List<KeySuggester.KeySuggestion> suggestions = KeySuggester.suggestKeys(data, headers);

            KeySuggestionDialog dialog = new KeySuggestionDialog(this, suggestions);
            dialog.setVisible(true);

            if (dialog.isAccepted()) {
                List<KeySuggester.KeySuggestion> selected = dialog.getSelectedSuggestions();
                columnMappingPanel.selectKeys(selected.stream().map(KeySuggester.KeySuggestion::getColumnName).collect(Collectors.toList()));
            }
            statusLabel.setText("Ready.");
        } catch (Exception e) {
            statusLabel.setText("Error during key suggestion.");
            JOptionPane.showMessageDialog(this, "Error suggesting keys: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
}
