package com.excelutility.gui;

import com.excelutility.core.ConcatenationMode;
import com.excelutility.core.FilterGroup;
import com.excelutility.core.FilterManager;
import com.excelutility.io.ExcelReader;
import net.miginfocom.swing.MigLayout;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FileConfigPanel extends JPanel {

    private final JTextField fileField = new JTextField();
    private final JComboBox<String> sheetCombo = new JComboBox<>();
    private final JRadioButton singleHeaderRadio = new JRadioButton("Single Header (Row 1)", true);
    private final JRadioButton multiHeaderRadio = new JRadioButton("Multi-row Header");
    private final JButton detectHeaderButton;
    private final JButton autoSuggestButton;
    private final JButton filterButton;
    private final JButton previewButton;

    private FilterGroup activeFilterGroup;
    private List<Integer> headerRowIndices = new ArrayList<>(Collections.singletonList(0));
    private ConcatenationMode concatenationMode = ConcatenationMode.LEAF_ONLY;
    private List<String> availableColumns = new ArrayList<>();
    private File selectedFile;
    private final Component parent;

    public FileConfigPanel(String title, Component parent) {
        this.parent = parent;
        setLayout(new MigLayout("fillx, insets 15, wrap 2", "[pref!][grow, fill]", "[]5[]10[]5[]10[]"));

        JLabel headerLabel = new JLabel(title);
        headerLabel.setFont(UIConstants.FONT_SUBHEADING);
        headerLabel.setForeground(UIConstants.COLOR_TEXT_HEADER);
        add(headerLabel, "span, gaptop 5, gapbottom 10");

        add(new JLabel("File Path:"));
        JPanel filePanel = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill][][]"));
        fileField.setEditable(true);
        filePanel.add(fileField, "growx");
        JButton openButton = new JButton("Browse...");
        previewButton = new JButton("Preview");
        openButton.addActionListener(e -> selectFile());
        filePanel.add(openButton);
        filePanel.add(previewButton);
        add(filePanel, "span, growx, wrap");

        add(new JLabel("Sheet:"));
        sheetCombo.addActionListener(e -> {
            if (e.getActionCommand().equals("comboBoxChanged")) {
                loadHeadersForFilter();
            }
        });
        add(sheetCombo, "growx, wrap");

        add(new JLabel("Header:"), "gaptop 10");
        JPanel headerPanel = new JPanel(new MigLayout("fillx, insets 0", "[][]"));
        ButtonGroup headerGroup = new ButtonGroup();
        headerGroup.add(singleHeaderRadio);
        headerGroup.add(multiHeaderRadio);
        headerPanel.add(singleHeaderRadio);
        headerPanel.add(multiHeaderRadio);
        detectHeaderButton = new JButton("Detect Headers...");
        detectHeaderButton.addActionListener(e -> detectHeader());
        headerPanel.add(detectHeaderButton, "gapleft 20");
        add(headerPanel, "wrap");

        add(new JLabel("Actions:"), "gaptop 10, aligny top");
        JPanel actionPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill][grow, fill]"));
        autoSuggestButton = new JButton("Auto-Suggest Keys");
        filterButton = new JButton("Set Pre-Filter...");
        filterButton.addActionListener(e -> openFilterBuilder());
        actionPanel.add(autoSuggestButton);
        actionPanel.add(filterButton);
        add(actionPanel, "wrap");
    }

    private void detectHeader() {
        if (selectedFile == null || getSelectedSheet() == null) {
            JOptionPane.showMessageDialog(this, "Please select a file and sheet first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try (FileInputStream fis = new FileInputStream(selectedFile);
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheet(getSelectedSheet());
            if (sheet != null) {
                HeaderDetectionDialog dialog = new HeaderDetectionDialog((Frame) SwingUtilities.getWindowAncestor(this), sheet);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    this.headerRowIndices = dialog.getSelectedHeaderRowIndices();
                    this.concatenationMode = dialog.getConcatenationMode();
                    multiHeaderRadio.setSelected(true);
                    JOptionPane.showMessageDialog(this, "Header rows set to: " + headerRowIndices.stream().map(i -> i + 1).collect(Collectors.toList()).toString(), "Header Detection", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error reading file for header detection: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selectFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Excel File");
        FileNameExtensionFilter excelFilter = new FileNameExtensionFilter("Excel Files (*.xls, *.xlsx)", "xls", "xlsx");
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
                loadHeadersForFilter();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent, "Error reading sheets from file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadHeadersForFilter() {
        try {
            if (getFilePath() != null && !getFilePath().isEmpty() && getSelectedSheet() != null) {
                List<List<Object>> headerData = ExcelReader.readPreview(getFilePath(), getSelectedSheet(), 20);
                if (!headerData.isEmpty()) {
                    this.availableColumns = headerData.get(getHeaderRowIndices().get(0)).stream().map(Object::toString).collect(Collectors.toList());
                } else {
                    this.availableColumns = new ArrayList<>();
                }
            }
        } catch (Exception e) {
            this.availableColumns = new ArrayList<>();
        }
    }

    private void openFilterBuilder() {
        if (availableColumns.isEmpty()) {
            loadHeadersForFilter();
        }
        if (availableColumns.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Could not load column headers. Please check the file and sheet selection.", "No Columns Found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        FilterBuilderDialog dialog = new FilterBuilderDialog((Frame) SwingUtilities.getWindowAncestor(this), availableColumns);
        dialog.setVisible(true);

        if (dialog.isApplied()) {
            this.activeFilterGroup = dialog.getFilterGroup();
            JOptionPane.showMessageDialog(this, "Filter has been set and will be applied on the next comparison.", "Filter Set", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public List<Integer> getHeaderRowIndices() {
        if (multiHeaderRadio.isSelected()) {
            return headerRowIndices;
        }
        return new ArrayList<>(Collections.singletonList(0));
    }

    public void setHeaderRowIndices(List<Integer> indices) { this.headerRowIndices = indices; multiHeaderRadio.setSelected(true); }
    public ConcatenationMode getConcatenationMode() { return concatenationMode; }
    public void setConcatenationMode(ConcatenationMode mode) { this.concatenationMode = mode; }
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
}
