package com.excelutility.gui;

import javax.swing.*;
import java.awt.*;

public class StartupModeDialog extends JDialog {

    public enum Mode {
        COMPARE,
        FILTER,
        NONE
    }

    private Mode selectedMode = Mode.NONE;

    public StartupModeDialog(Frame owner) {
        super(owner, "Select Mode", true);
        setLayout(new BorderLayout(10, 10));

        JLabel label = new JLabel("Please select the operation you want to perform:", SwingConstants.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(label, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        JButton compareButton = new JButton("Compare Excel Files");
        JButton filterButton = new JButton("Filter Excel Data");

        compareButton.addActionListener(e -> {
            selectedMode = Mode.COMPARE;
            setVisible(false);
        });

        filterButton.addActionListener(e -> {
            selectedMode = Mode.FILTER;
            setVisible(false);
        });

        buttonPanel.add(compareButton);
        buttonPanel.add(filterButton);
        add(buttonPanel, BorderLayout.CENTER);

        setSize(400, 150);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    public Mode getSelectedMode() {
        return selectedMode;
    }
}
