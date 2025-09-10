package com.excelutility.gui;

import com.excelutility.io.ExcelReader;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileSelectionPanel extends JPanel {

    private final JTextField fileField = new JTextField();
    private final JComboBox<String> sheetCombo = new JComboBox<>();
    private File selectedFile;
    private final Component parent;

    public FileSelectionPanel(String title, Component parent) {
        this.parent = parent;
        setLayout(new MigLayout("fillx", "[][grow][]", ""));
        setBorder(BorderFactory.createTitledBorder(title));

        JButton openButton = new JButton("Browse...");

        add(new JLabel("File:"));
        add(fileField, "growx");
        add(openButton, "wrap");
        add(new JLabel("Sheet:"));
        add(sheetCombo, "growx, span 2");

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

    public List<String> getColumnNames() {
        if (selectedFile == null || getSelectedSheet() == null) {
            return new ArrayList<>();
        }
        try {
            List<List<Object>> preview = ExcelReader.readPreview(getFilePath(), getSelectedSheet(), 1);
            if (preview.isEmpty()) {
                return new ArrayList<>();
            }
            List<String> headers = new ArrayList<>();
            for (Object header : preview.get(0)) {
                headers.add(header != null ? header.toString() : "");
            }
            return headers;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent, "Error reading headers from file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return new ArrayList<>();
        }
    }

    public String getFilePath() {
        return fileField.getText();
    }

    public String getSelectedSheet() {
        return sheetCombo.getSelectedItem() != null ? sheetCombo.getSelectedItem().toString() : null;
    }

    public JComboBox<String> getSheetCombo() {
        return sheetCombo;
    }
}
