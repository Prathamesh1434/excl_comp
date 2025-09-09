package com.excelutility.gui;

import com.excelutility.compare.ComparisonReport;
import com.excelutility.compare.KeyMatcher;
import com.excelutility.excel.ExcelReader;
import com.excelutility.excel.ExcelWriter;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import com.excelutility.compare.ComparisonReport;
import com.excelutility.compare.KeyMatcher;
import com.excelutility.excel.ExcelReader;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The main window of the Excel Utility application.
 * It provides components for loading Excel files, selecting sheets,
 * and initiating the comparison.
 */
public class MainFrame extends JFrame {

    private JButton loadFile1Button, loadFile2Button, compareButton;
    private JLabel file1Label, file2Label;
    private JComboBox<String> sheet1ComboBox, sheet2ComboBox;
    private File file1, file2;
    private PreviewPanel previewPanel1, previewPanel2;
    private JList<String> keyColumnList;
    private DefaultListModel<String> keyColumnListModel;
    private JTable resultTable;
    private ResultTableModel resultTableModel;
    private ComparisonReport lastReport;

    /**
     * Constructs the main frame and initializes its components.
     */
    public MainFrame() {
        setTitle("Excel Utility");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();
    }

    /**
     * Initializes the UI components.
     */
    private void initComponents() {
        loadFile1Button = new JButton("Load File 1");
        loadFile2Button = new JButton("Load File 2");
        compareButton = new JButton("Compare");
        compareButton.setEnabled(false);

        file1Label = new JLabel("No file selected.");
        file2Label = new JLabel("No file selected.");

        sheet1ComboBox = new JComboBox<>();
        sheet2ComboBox = new JComboBox<>();

        previewPanel1 = new PreviewPanel("File 1 Preview");
        previewPanel2 = new PreviewPanel("File 2 Preview");

        keyColumnListModel = new DefaultListModel<>();
        keyColumnList = new JList<>(keyColumnListModel);
        keyColumnList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        keyColumnList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                compareButton.setEnabled(!keyColumnList.isSelectionEmpty());
            }
        });

        resultTableModel = new ResultTableModel();
        resultTable = new JTable(resultTableModel);
        resultTable.setDefaultRenderer(Object.class, new ResultTableRenderer());

        loadFile1Button.addActionListener(e -> loadFile(1));
        loadFile2Button.addActionListener(e -> loadFile(2));

        sheet1ComboBox.addActionListener(e -> updatePreview(1));
        sheet2ComboBox.addActionListener(e -> updatePreview(2));

        compareButton.addActionListener(e -> performComparison());

        JButton exportButton = new JButton("Export to Excel");
        exportButton.addActionListener(e -> exportReport());
    }

    /**
     * Lays out the components in the frame using GridBagLayout.
     */
    private void layoutComponents() {
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // File 1 Controls
        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(loadFile1Button, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        topPanel.add(file1Label, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        topPanel.add(sheet1ComboBox, gbc);

        // File 2 Controls
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        topPanel.add(loadFile2Button, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        topPanel.add(file2Label, gbc);

        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        topPanel.add(sheet2ComboBox, gbc);

        JSplitPane previewSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(previewPanel1), new JScrollPane(previewPanel2));
        previewSplitPane.setResizeWeight(0.5);

        JPanel keySelectionPanel = new JPanel(new BorderLayout());
        keySelectionPanel.setBorder(BorderFactory.createTitledBorder("Key Columns"));
        keySelectionPanel.add(new JScrollPane(keyColumnList), BorderLayout.CENTER);

        JSplitPane topSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, previewSplitPane, keySelectionPanel);
        topSplitPane.setResizeWeight(0.8);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Comparison Results"));
        bottomPanel.add(new JScrollPane(resultTable), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(compareButton);
        JButton exportButton = new JButton("Export to Excel");
        exportButton.addActionListener(e -> exportReport());
        buttonPanel.add(exportButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplitPane, bottomPanel);
        mainSplitPane.setResizeWeight(0.5);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(mainSplitPane, BorderLayout.CENTER);
    }

    /**
     * Handles the file loading process.
     *
     * @param fileNumber 1 for the first file, 2 for the second file.
     */
    private void loadFile(int fileNumber) {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Excel Files", "xls", "xlsx");
        fileChooser.setFileFilter(filter);

        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                ExcelReader reader = new ExcelReader(selectedFile.getAbsolutePath());
                List<String> sheetNames = reader.getSheetNames();
                reader.close();

                if (fileNumber == 1) {
                    file1 = selectedFile;
                    file1Label.setText(file1.getName());
                    sheet1ComboBox.removeAllItems();
                    for (String sheetName : sheetNames) {
                        sheet1ComboBox.addItem(sheetName);
                    }
                } else {
                    file2 = selectedFile;
                    file2Label.setText(file2.getName());
                    sheet2ComboBox.removeAllItems();
                    for (String sheetName : sheetNames) {
                        sheet2ComboBox.addItem(sheetName);
                    }
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error reading Excel file: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Updates the preview panel for the selected sheet.
     *
     * @param fileNumber 1 for the first file, 2 for the second file.
     */
    private void updatePreview(int fileNumber) {
        File file;
        JComboBox<String> sheetComboBox;
        PreviewPanel previewPanel;

        if (fileNumber == 1) {
            file = file1;
            sheetComboBox = sheet1ComboBox;
            previewPanel = previewPanel1;
        } else {
            file = file2;
            sheetComboBox = sheet2ComboBox;
            previewPanel = previewPanel2;
        }

        if (file == null || sheetComboBox.getSelectedItem() == null) {
            return;
        }

        String selectedSheet = sheetComboBox.getSelectedItem().toString();

        try {
            ExcelReader reader = new ExcelReader(file.getAbsolutePath());
            List<List<Object>> allData = reader.getSheetData(selectedSheet);
            reader.close();

            List<Object> headers = new ArrayList<>();
            if (!allData.isEmpty()) {
                headers.addAll(allData.get(0));
            }

            if (fileNumber == 1) {
                keyColumnListModel.clear();
                for (Object header : headers) {
                    keyColumnListModel.addElement(header.toString());
                }
            }

            List<List<Object>> previewData = new ArrayList<>();
            int rowCount = Math.min(allData.size(), 11);
            for (int i = 1; i < rowCount; i++) {
                previewData.add(allData.get(i));
            }

            previewPanel.updateData(previewData, headers);

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error reading Excel file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performComparison() {
        if (file1 == null || file2 == null || sheet1ComboBox.getSelectedItem() == null || sheet2ComboBox.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Please select two files and sheets.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Integer> selectedKeyIndices = Arrays.stream(keyColumnList.getSelectedIndices()).boxed().collect(Collectors.toList());
        if (selectedKeyIndices.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select at least one key column.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            ExcelReader reader1 = new ExcelReader(file1.getAbsolutePath());
            List<List<Object>> data1 = reader1.getSheetData(sheet1ComboBox.getSelectedItem().toString());
            reader1.close();

            ExcelReader reader2 = new ExcelReader(file2.getAbsolutePath());
            List<List<Object>> data2 = reader2.getSheetData(sheet2ComboBox.getSelectedItem().toString());
            reader2.close();

            // Header check
            if (!data1.isEmpty() && !data2.isEmpty()) {
                List<Object> headers1 = data1.get(0);
                List<Object> headers2 = data2.get(0);
                if (!headers1.equals(headers2)) {
                    int response = JOptionPane.showConfirmDialog(this,
                            "The headers of the two sheets do not match. Continue anyway?",
                            "Header Mismatch",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (response == JOptionPane.NO_OPTION) {
                        return;
                    }
                }
            }

            this.lastReport = KeyMatcher.matchAndCompare(data1, data2, selectedKeyIndices);
            resultTableModel.setReport(lastReport);

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error during comparison: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportReport() {
        if (lastReport == null) {
            JOptionPane.showMessageDialog(this, "Please run a comparison first.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Report");
        fileChooser.setSelectedFile(new File("comparison_result.xlsx"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files (*.xlsx)", "xlsx"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                ExcelWriter.writeReport(lastReport, fileToSave.getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Report exported successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error exporting report: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
