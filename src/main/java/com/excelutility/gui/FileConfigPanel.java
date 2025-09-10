package com.excelutility.gui;

import net.miginfocom.swing.MigLayout;
import javax.swing.*;

public class FileConfigPanel extends JPanel {

    private final JTextField fileField = new JTextField();
    private final JComboBox<String> sheetCombo = new JComboBox<>();
    private final JRadioButton noHeaderRadio = new JRadioButton("No Header");
    private final JRadioButton singleHeaderRadio = new JRadioButton("Single Header Row", true);
    private final JRadioButton multiHeaderRadio = new JRadioButton("Multi-row Header");
    private final JSpinner singleHeaderSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));

    public FileConfigPanel(String title) {
        setLayout(new MigLayout("fillx", "[][grow][]", ""));
        setBorder(BorderFactory.createTitledBorder(title));

        // File and Sheet selection
        fileField.setEditable(false);
        JButton openButton = new JButton("...");
        add(new JLabel("File:"));
        add(fileField, "growx");
        add(openButton, "wrap");
        add(new JLabel("Sheet:"));
        add(sheetCombo, "growx, span 2, wrap");

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
        JButton autoSuggestButton = new JButton("Auto-Suggest Keys");
        JButton filterButton = new JButton("Filter...");
        add(detectHeaderButton, "span, split 3, gaptop 5");
        add(autoSuggestButton);
        add(filterButton);
    }

    // Public methods to get/set values will be added here
    public JTextField getFileField() { return fileField; }
    public JComboBox<String> getSheetCombo() { return sheetCombo; }
    public JButton getAutoSuggestButton() {
        // This is a bit of a hack. A better way would be to pass an ActionListener to the constructor.
        return (JButton) ((JPanel) getComponent(4)).getComponent(1);
    }

    public JButton getFilterButton() {
        return (JButton) ((JPanel) getComponent(4)).getComponent(2);
    }
}
