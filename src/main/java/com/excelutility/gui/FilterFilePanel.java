package com.excelutility.gui;

import com.excelutility.core.ConcatenationMode;
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
import java.util.List;
import com.excelutility.core.CanonicalNameBuilder;


public class FilterFilePanel extends JPanel {

    private final JTextField fileField = new JTextField();
    private final JComboBox<String> sheetCombo = new JComboBox<>();
    private final JButton detectHeaderButton;

    private List<Integer> headerRowIndices = new ArrayList<>();
    private ConcatenationMode concatenationMode = ConcatenationMode.LEAF_ONLY;
    private File selectedFile;
    private final Component parent;

    public FilterFilePanel(String title, Component parent) {
        this.parent = parent;
        setLayout(new MigLayout("fillx", "[][grow][]", ""));
        setBorder(BorderFactory.createTitledBorder(title));

        fileField.setEditable(false);
        JButton openButton = new JButton("Browse...");
        detectHeaderButton = new JButton("Detect Header");

        add(new JLabel("File:"));
        add(fileField, "growx");
        add(openButton, "wrap");
        add(new JLabel("Sheet:"));
        add(sheetCombo, "growx, span 2, wrap");
        add(detectHeaderButton, "span, growx, gaptop 5");

        openButton.addActionListener(e -> selectFile());
        detectHeaderButton.addActionListener(e -> detectHeader());
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
                    JOptionPane.showMessageDialog(this, "Header rows set to: " + headerRowIndices.toString(), "Header Detection", JOptionPane.INFORMATION_MESSAGE);
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
        try (Workbook workbook = WorkbookFactory.create(selectedFile)) {
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
    public List<Integer> getHeaderRowIndices() { return headerRowIndices; }
    public ConcatenationMode getConcatenationMode() { return concatenationMode; }
}
