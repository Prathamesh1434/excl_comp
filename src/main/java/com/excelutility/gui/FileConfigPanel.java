package com.excelutility.gui;

import com.excelutility.core.FilterGroup;
import com.excelutility.core.FilterManager;
import com.excelutility.io.ExcelReader;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class FileConfigPanel extends JPanel {

    private final JTextField fileField = new JTextField();
    private final JComboBox<String> sheetCombo = new JComboBox<>();
    private final JRadioButton noHeaderRadio = new JRadioButton("No Header");
    private final JRadioButton singleHeaderRadio = new JRadioButton("Single Header Row", true);
    private final JRadioButton multiHeaderRadio = new JRadioButton("Multi-row Header");
    private final JSpinner singleHeaderSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
    private final JButton autoSuggestButton;
    private final JButton filterButton;
    private final JButton openButton;
    private final JButton previewButton;
    private final JComboBox<String> savedFiltersCombo;
    private final JButton loadFilterButton;

    private FilterGroup activeFilterGroup;
    private List<String> availableColumns;
    private File selectedFile;
    private final Component parent;

    public FileConfigPanel(String title, Component parent) {
        this.parent = parent;
        setLayout(new MigLayout("fillx", "[][grow][][]", ""));
        setBorder(BorderFactory.createTitledBorder(title));

        // --- UI Components ---
        fileField.setEditable(true);
        openButton = new JButton("Browse...");
        previewButton = new JButton("Preview");
        autoSuggestButton = new JButton("Auto-Suggest Keys");
        filterButton = new JButton("Filter...");
        savedFiltersCombo = new JComboBox<>();
        loadFilterButton = new JButton("Load");

        // --- Layout ---
        add(new JLabel("File:"));
        add(fileField, "growx");
        add(openButton);
        add(previewButton, "wrap");

        add(new JLabel("Sheet:"));
        add(sheetCombo, "growx, span 3, wrap");

        add(new JSeparator(), "span, growx, wrap, gaptop 5");
        add(new JLabel("Header Options:"), "span, wrap, gaptop 5");

        ButtonGroup headerGroup = new ButtonGroup();
        headerGroup.add(noHeaderRadio);
        headerGroup.add(singleHeaderRadio);
        headerGroup.add(multiHeaderRadio);

        add(noHeaderRadio, "split 3");
        add(singleHeaderRadio);
        add(multiHeaderRadio, "wrap");
        add(new JLabel("Row Index:"), "gapleft 20");
        add(singleHeaderSpinner, "wrap");

        add(new JSeparator(), "span, growx, gaptop 5");
        add(new JLabel("Actions:"), "span, split 3, gaptop 5");
        add(autoSuggestButton);
        add(filterButton, "wrap");

        add(new JLabel("Saved Filters:"), "span, split 3, gaptop 5");
        add(savedFiltersCombo, "growx");
        add(loadFilterButton);

        // --- Action Listeners ---
        openButton.addActionListener(e -> selectFile());
        filterButton.addActionListener(e -> openFilterBuilder());
        loadFilterButton.addActionListener(e -> loadSelectedFilter());

        noHeaderRadio.addActionListener(e -> singleHeaderSpinner.setEnabled(false));
        singleHeaderRadio.addActionListener(e -> singleHeaderSpinner.setEnabled(true));
        multiHeaderRadio.addActionListener(e -> singleHeaderSpinner.setEnabled(false));

        updateSavedFilters();
    }

    private void selectFile() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter excelFilter = new FileNameExtensionFilter("Excel Files (*.xls, *.xlsx)", "xls", "xlsx");
        chooser.addChoosableFileFilter(excelFilter);
        chooser.setFileFilter(excelFilter);
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            this.selectedFile = chooser.getSelectedFile();
            fileField.setText(selectedFile.getAbsolutePath());
            populateSheetCombo();
        }
    }

    private void populateSheetCombo() {
        if (selectedFile == null) return;
        try {
            List<String> sheetNames = ExcelReader.getSheetNames(selectedFile.getAbsolutePath());
            sheetCombo.removeAllItems();
            for (String name : sheetNames) {
                sheetCombo.addItem(name);
            }
            if (!sheetNames.isEmpty()) {
                sheetCombo.setSelectedIndex(0);
                // Automatically load headers for the first sheet
                loadHeadersForFilter();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent, "Error reading sheets from file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadHeadersForFilter() {
        try {
            if (getFilePath() != null && !getFilePath().isEmpty() && getSelectedSheet() != null) {
                List<List<Object>> headerData = ExcelReader.read(getFilePath(), getSelectedSheet(), true);
                if (!headerData.isEmpty()) {
                    this.availableColumns = headerData.get(0).stream().map(Object::toString).collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            // Suppress error for this background operation
        }
    }

    private void openFilterBuilder() {
        if (availableColumns == null || availableColumns.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please load a file and select a sheet first.", "No Columns Found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        FilterBuilderDialog dialog = new FilterBuilderDialog((Frame) SwingUtilities.getWindowAncestor(this), availableColumns);
        dialog.setVisible(true);

        if (dialog.isApplied()) {
            this.activeFilterGroup = dialog.getFilterGroup();
            // Potentially update a label to show that a filter is active
            JOptionPane.showMessageDialog(this, "Filter applied.", "Filter", JOptionPane.INFORMATION_MESSAGE);
        }
        // After dialog is closed, one or more filters might have been saved, so refresh the list.
        updateSavedFilters();
    }

    private void loadSelectedFilter() {
        String selectedFilterName = (String) savedFiltersCombo.getSelectedItem();
        if (selectedFilterName == null) {
            JOptionPane.showMessageDialog(this, "No saved filter selected.", "Load Filter", JOptionPane.WARNING_MESSAGE);
            return;
        }
        this.activeFilterGroup = FilterManager.getInstance().getFilter(selectedFilterName);
        JOptionPane.showMessageDialog(this, "Filter '" + selectedFilterName + "' loaded and is ready to be applied on next comparison.", "Filter Loaded", JOptionPane.INFORMATION_MESSAGE);
    }

    public void updateSavedFilters() {
        savedFiltersCombo.removeAllItems();
        FilterManager.getInstance().getSavedFilterNames().forEach(savedFiltersCombo::addItem);
    }

    // --- Public Getters and Setters ---
    public String getFilePath() { return fileField.getText(); }
    public String getSelectedSheet() { return sheetCombo.getSelectedItem() != null ? sheetCombo.getSelectedItem().toString() : null; }
    public void setFilePath(String path) {
        if (path != null && !path.isEmpty()) {
            this.selectedFile = new File(path);
            fileField.setText(path);
            populateSheetCombo();
        } else {
            this.selectedFile = null;
            fileField.setText("");
            sheetCombo.removeAllItems();
        }
    }
    public void setSelectedSheet(String sheetName) { sheetCombo.setSelectedItem(sheetName); }
    public FilterGroup getActiveFilterGroup() { return activeFilterGroup; }
    public JComboBox<String> getSheetCombo() { return sheetCombo; }
    public JButton getAutoSuggestButton() { return autoSuggestButton; }
    public JButton getPreviewButton() { return previewButton; }
    public void addSheetSelectionListener(java.awt.event.ActionListener listener) { sheetCombo.addActionListener(listener); }
}
