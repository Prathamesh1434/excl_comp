package com.excelcomparator.gui;

import com.excelcomparator.compare.Comparer;
import com.excelcomparator.compare.ComparisonResult;
import com.excelcomparator.excel.ExcelReader;
import com.excelcomparator.excel.ExcelWriter;

import com.excelcomparator.compare.Comparer;
import com.excelcomparator.compare.ComparisonResult;
import com.excelcomparator.excel.ExcelReader;
import com.excelcomparator.excel.Normalizer;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The main window of the Excel Comparator application.
 */
public class MainFrame extends JFrame {

    private JButton loadFile1Button, loadFile2Button, runComparisonButton;
    private JLabel file1Label, file2Label;
    private PreviewTableModel previewTableModel1, previewTableModel2;
    private JCheckBox normalizeCheckbox1, normalizeCheckbox2;
    private JSpinner headerRowsSpinner1, headerRowsSpinner2;
    private File file1, file2;
    private KeyMappingPanel keyMappingPanel;
    private JTable resultsTable;
    private ResultTableModel resultsTableModel;
    private ComparisonResult lastComparisonResult;

    public MainFrame() {
        setTitle("Excel Comparator");
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
    }

    private void initComponents() {
        // Main container with BorderLayout
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout(10, 10));

        // 1. Top Row: Normalization Checkboxes
        JPanel normalizationPanel = new JPanel(new GridLayout(1, 2));
        normalizationPanel.setBorder(BorderFactory.createTitledBorder("Normalization"));

        JPanel normPanel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        normalizeCheckbox1 = new JCheckBox("Enable Normalization for File 1");
        headerRowsSpinner1 = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        normPanel1.add(normalizeCheckbox1);
        normPanel1.add(new JLabel("Header Rows:"));
        normPanel1.add(headerRowsSpinner1);
        normalizationPanel.add(normPanel1);

        JPanel normPanel2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        normalizeCheckbox2 = new JCheckBox("Enable Normalization for File 2");
        headerRowsSpinner2 = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        normPanel2.add(normalizeCheckbox2);
        normPanel2.add(new JLabel("Header Rows:"));
        normPanel2.add(headerRowsSpinner2);
        normalizationPanel.add(normPanel2);

        contentPane.add(normalizationPanel, BorderLayout.NORTH);

        // 2. Bottom Row: Action Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        runComparisonButton = new JButton("Run Comparison");
        runComparisonButton.setEnabled(false);
        runComparisonButton.addActionListener(e -> performComparison());
        actionPanel.add(runComparisonButton);
        JButton exportButton = new JButton("Export to Excel");
        exportButton.addActionListener(e -> exportReport());
        actionPanel.add(exportButton);

        JButton clearButton = new JButton("Clear Result");
        clearButton.addActionListener(e -> clearResults());
        actionPanel.add(clearButton);

        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> System.exit(0));
        actionPanel.add(exitButton);

        contentPane.add(actionPanel, BorderLayout.SOUTH);

        // --- Center Area ---

        // 3. Middle Row: Preview Section
        JPanel previewSectionPanel = new JPanel(new BorderLayout());

        // File loading controls
        JPanel fileLoadPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        loadFile1Button = new JButton("Load File 1");
        file1Label = new JLabel("No file selected.");
        loadFile1Button.addActionListener(e -> loadFile(1));
        JPanel file1Panel = new JPanel();
        file1Panel.add(loadFile1Button);
        file1Panel.add(file1Label);
        fileLoadPanel.add(file1Panel);

        loadFile2Button = new JButton("Load File 2");
        file2Label = new JLabel("No file selected.");
        loadFile2Button.addActionListener(e -> loadFile(2));
        JPanel file2Panel = new JPanel();
        file2Panel.add(loadFile2Button);
        file2Panel.add(file2Label);
        fileLoadPanel.add(file2Panel);

        previewSectionPanel.add(fileLoadPanel, BorderLayout.NORTH);

        // Preview tables
        previewTableModel1 = new PreviewTableModel();
        JTable previewTable1 = new JTable(previewTableModel1);
        previewTableModel2 = new PreviewTableModel();
        JTable previewTable2 = new JTable(previewTableModel2);

        JSplitPane previewSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(previewTable1),
                new JScrollPane(previewTable2));
        previewSplit.setResizeWeight(0.5);
        previewSectionPanel.add(previewSplit, BorderLayout.CENTER);

        // 4. Bottom Section (Options and Results)
        keyMappingPanel = new KeyMappingPanel();
        keyMappingPanel.setBorder(BorderFactory.createTitledBorder("Key Column Mappings"));

        resultsTableModel = new ResultTableModel();
        resultsTable = new JTable(resultsTableModel);
        resultsTable.setDefaultRenderer(Object.class, new ResultTableRenderer());
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Results"));
        resultsPanel.add(new JScrollPane(resultsTable), BorderLayout.CENTER);

        JSplitPane bottomSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, keyMappingPanel, resultsPanel);
        bottomSplit.setResizeWeight(0.4);

        // Combine Preview and Bottom sections
        JSplitPane mainVerticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, previewSectionPanel, bottomSplit);
        mainVerticalSplit.setResizeWeight(0.5);

        contentPane.add(mainVerticalSplit, BorderLayout.CENTER);
    }

    private void loadFile(int fileNumber) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files", "xls", "xlsx"));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                ExcelReader reader = new ExcelReader(selectedFile.getAbsolutePath());
                // For now, just load from the first sheet. Sheet selection will be added later.
                String firstSheet = reader.getSheetNames().get(0);
                List<List<Object>> originalData = reader.getSheetData(firstSheet);
                reader.close();

                List<List<Object>> processedData = new ArrayList<>(originalData);
                if (fileNumber == 1 && normalizeCheckbox1.isSelected()) {
                    processedData = Normalizer.normalizeHeaders(originalData, (int) headerRowsSpinner1.getValue());
                } else if (fileNumber == 2 && normalizeCheckbox2.isSelected()) {
                    processedData = Normalizer.normalizeHeaders(originalData, (int) headerRowsSpinner2.getValue());
                }

                List<String> headers = new ArrayList<>();
                if(!processedData.isEmpty()){
                    headers = processedData.get(0).stream().map(Object::toString).collect(Collectors.toList());
                }

                List<List<Object>> previewData = new ArrayList<>();
                previewData.add(processedData.get(0)); // header
                previewData.addAll(processedData.subList(1, Math.min(processedData.size(), 11)));


                if (fileNumber == 1) {
                    file1 = selectedFile;
                    file1Label.setText(file1.getName());
                    previewTableModel1.setData(previewData);
                    keyMappingPanel.setFile1Columns(headers);
                } else {
                    file2 = selectedFile;
                    file2Label.setText(file2.getName());
                    previewTableModel2.setData(previewData);
                    keyMappingPanel.setFile2Columns(headers);
                }

                if (file1 != null && file2 != null) {
                    runComparisonButton.setEnabled(true);
                }

            } catch (IOException | IndexOutOfBoundsException ex) {
                JOptionPane.showMessageDialog(this, "Error reading file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                if (fileNumber == 1) {
                    file1Label.setText("No file selected.");
                    previewTableModel1.clearData();
                } else {
                    file2Label.setText("No file selected.");
                    previewTableModel2.clearData();
                }
            }
        }
    }

    private void performComparison() {
        if (keyMappingPanel.getKeyMappings().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please define at least one key column mapping.", "No Key Mapping", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            ExcelReader reader1 = new ExcelReader(file1.getAbsolutePath());
            List<List<Object>> data1 = reader1.getSheetData(reader1.getSheetNames().get(0));
            reader1.close();

            ExcelReader reader2 = new ExcelReader(file2.getAbsolutePath());
            List<List<Object>> data2 = reader2.getSheetData(reader2.getSheetNames().get(0));
            reader2.close();

            if (normalizeCheckbox1.isSelected()) {
                data1 = Normalizer.normalizeHeaders(data1, (int) headerRowsSpinner1.getValue());
            }
            if (normalizeCheckbox2.isSelected()) {
                data2 = Normalizer.normalizeHeaders(data2, (int) headerRowsSpinner2.getValue());
            }

            ComparisonResult result = Comparer.compare(
                    data1,
                    data2,
                    keyMappingPanel.getKeyMappings(),
                    keyMappingPanel.isColumnLevelComparisonEnabled(),
                    keyMappingPanel.isRowPresenceCheckEnabled()
            );

            this.lastComparisonResult = result;
            resultsTableModel.setComparisonResult(result);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error during comparison: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportReport() {
        if (lastComparisonResult == null) {
            JOptionPane.showMessageDialog(this, "Please run a comparison first.", "No Report", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Report");
        fileChooser.setSelectedFile(new File("comparison-report-" + System.currentTimeMillis() + ".xlsx"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files (*.xlsx)", "xlsx"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                ExcelWriter.write(lastComparisonResult, fileToSave.getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Report exported successfully!", "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error exporting report: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearResults() {
        file1 = null;
        file2 = null;
        file1Label.setText("No file selected.");
        file2Label.setText("No file selected.");
        previewTableModel1.clearData();
        previewTableModel2.clearData();
        resultsTableModel.clear();
        keyMappingPanel.clear();
        lastComparisonResult = null;
        runComparisonButton.setEnabled(false);
    }
}
