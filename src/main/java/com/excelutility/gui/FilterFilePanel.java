package com.excelutility.gui;

import com.excelutility.core.CanonicalNameBuilder;
import com.excelutility.core.ConcatenationMode;
import com.excelutility.io.ExcelReader;
import net.miginfocom.swing.MigLayout;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FilterFilePanel extends JPanel {

    private final JTextField fileField = new JTextField();
    private final JComboBox<String> sheetCombo = new JComboBox<>();
    private final JButton detectHeaderButton;
    private final JTextField searchField = new JTextField();

    private List<Integer> headerRowIndices = new ArrayList<>(Collections.singletonList(0));
    private ConcatenationMode concatenationMode = ConcatenationMode.LEAF_ONLY;
    private File selectedFile;
    private final Component parent;
    private List<String> allSheetNames = new ArrayList<>();

    public FilterFilePanel(String title, Component parent) {
        this.parent = parent;
        setLayout(new MigLayout("fillx, insets 15, wrap 2", "[pref!][grow, fill]"));

        // Modern header label
        JLabel headerLabel = new JLabel(title);
        headerLabel.setFont(UIConstants.FONT_SUBHEADING);
        headerLabel.setForeground(UIConstants.COLOR_TEXT_HEADER);
        add(headerLabel, "span, gaptop 5, gapbottom 10, wrap");

        // --- File Selection ---
        add(new JLabel("File Path:"));
        JPanel filePanel = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill][]"));
        fileField.setEditable(false); // Make it read-only, only populated by chooser
        filePanel.add(fileField, "growx");
        JButton openButton = new JButton("Browse...");
        openButton.addActionListener(e -> selectFile());
        filePanel.add(openButton);
        add(filePanel, "span, growx, wrap");

        // --- Sheet Selection ---
        add(new JLabel("Search Sheet:"));
        add(searchField, "growx, wrap");

        add(new JLabel("Sheet Name:"));
        add(sheetCombo, "growx, wrap");

        // --- Header Configuration ---
        detectHeaderButton = new JButton("Detect Headers...");
        detectHeaderButton.addActionListener(e -> detectHeader());
        add(detectHeaderButton, "span, growx, gaptop 10");

        // --- Event Listeners ---
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterSheets(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterSheets(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterSheets(); }
        });
    }

    private void selectFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Excel File");
        FileNameExtensionFilter excelFilter = new FileNameExtensionFilter("Excel Files (*.xls, *.xlsx)", "xls", "xlsx");
        chooser.setFileFilter(excelFilter);
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            this.selectedFile = chooser.getSelectedFile();
            fileField.setText(selectedFile.getAbsolutePath());
            loadAllSheetNames();
            filterSheets();
        }
    }

    private void loadAllSheetNames() {
        if (selectedFile == null) return;
        try {
            this.allSheetNames = ExcelReader.getSheetNames(selectedFile.getAbsolutePath());
        } catch (IOException e) {
            this.allSheetNames = new ArrayList<>();
            JOptionPane.showMessageDialog(parent, "Error reading sheets from file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filterSheets() {
        String searchTerm = searchField.getText().toLowerCase();
        List<String> filteredSheets = allSheetNames.stream()
                .filter(sheet -> sheet.toLowerCase().contains(searchTerm))
                .collect(Collectors.toList());

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addAll(filteredSheets);
        sheetCombo.setModel(model);

        if (sheetCombo.getItemCount() > 0) {
            sheetCombo.setSelectedIndex(0);
        }
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
                    JOptionPane.showMessageDialog(this, "Header rows set to: " + headerRowIndices.stream().map(i->i+1).collect(Collectors.toList()).toString(), "Header Detection", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error reading file for header detection: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public List<String> getColumnNames() {
        if (selectedFile == null || getSelectedSheet() == null) {
            return new ArrayList<>();
        }
        try (Workbook workbook = WorkbookFactory.create(selectedFile)) { // Read-only for safety
            Sheet sheet = workbook.getSheet(getSelectedSheet());
            if (sheet == null) return new ArrayList<>();

            List<Integer> finalHeaderRows = headerRowIndices.isEmpty() ? List.of(0) : headerRowIndices;
            return CanonicalNameBuilder.buildCanonicalHeaders(sheet, finalHeaderRows, concatenationMode, " | ");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, "Error reading headers from file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return new ArrayList<>();
        }
    }

    public String getFilePath() { return fileField.getText(); }
    public String getSelectedSheet() { return sheetCombo.getSelectedItem() != null ? sheetCombo.getSelectedItem().toString() : null; }
    public List<Integer> getHeaderRowIndices() { return headerRowIndices.isEmpty() ? List.of(0) : headerRowIndices; }
    public ConcatenationMode getConcatenationMode() { return concatenationMode; }
}
