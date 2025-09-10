package com.excelutility.gui;

import com.excelutility.io.ExcelReader;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

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

    private File selectedFile;
    private final Component parent;

    public FileConfigPanel(String title, Component parent) {
        this.parent = parent;
        setLayout(new MigLayout("fillx", "[][grow][][]", ""));
        setBorder(BorderFactory.createTitledBorder(title));

        // File and Sheet selection
        fileField.setEditable(true); // Allow pasting paths
        openButton = new JButton("Browse...");
        previewButton = new JButton("Preview");

        add(new JLabel("File:"));
        add(fileField, "growx");
        add(openButton);
        add(previewButton, "wrap");

        add(new JLabel("Sheet:"));
        add(sheetCombo, "growx, span 3, wrap");

        // Header Selection
        add(new JSeparator(), "span, growx, wrap, gaptop 10");
        add(new JLabel("Header Options:"), "span, wrap");

        ButtonGroup headerGroup = new ButtonGroup();
        headerGroup.add(noHeaderRadio);
        headerGroup.add(singleHeaderRadio);
        headerGroup.add(multiHeaderRadio);

        add(noHeaderRadio, "split 3");
        add(singleHeaderRadio);
        add(multiHeaderRadio, "wrap");

        add(new JLabel("Row Index:"), "gapleft 20");
        add(singleHeaderSpinner, "wrap");

        // Action listeners to enable/disable spinner
        noHeaderRadio.addActionListener(e -> singleHeaderSpinner.setEnabled(false));
        singleHeaderRadio.addActionListener(e -> singleHeaderSpinner.setEnabled(true));
        multiHeaderRadio.addActionListener(e -> singleHeaderSpinner.setEnabled(false));

        JButton detectHeaderButton = new JButton("Detect Header");
        autoSuggestButton = new JButton("Auto-Suggest Keys");
        filterButton = new JButton("Filter...");
        add(detectHeaderButton, "span, split 3, gaptop 5");
        add(autoSuggestButton);
        add(filterButton);

        // Explicitly wiring up the action listener for the "Browse..." button.
        // This calls the selectFile() method within this panel.
        openButton.addActionListener(e -> selectFile());
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
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent, "Error reading sheets from file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public String getFilePath() {
        return fileField.getText();
    }

    public String getSelectedSheet() {
        return sheetCombo.getSelectedItem() != null ? sheetCombo.getSelectedItem().toString() : null;
    }

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

    public void setSelectedSheet(String sheetName) {
        sheetCombo.setSelectedItem(sheetName);
    }

    public JComboBox<String> getSheetCombo() { return sheetCombo; }
    public JButton getAutoSuggestButton() { return autoSuggestButton; }
    public JButton getFilterButton() { return filterButton; }
    public JButton getPreviewButton() { return previewButton; }

    public void addSheetSelectionListener(java.awt.event.ActionListener listener) {
        sheetCombo.addActionListener(listener);
    }
}
