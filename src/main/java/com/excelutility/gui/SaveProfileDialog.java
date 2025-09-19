package com.excelutility.gui;

import javax.swing.*;
import java.awt.*;

public class SaveProfileDialog extends JDialog {

    private JTextField profileNameField;
    private JCheckBox saveFilePathsCheckBox;
    private JTextArea descriptionArea;
    private boolean confirmed = false;

    public SaveProfileDialog(Frame owner) {
        super(owner, "Save Profile", true);
        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        formPanel.add(new JLabel("Profile Name:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        profileNameField = new JTextField(20);
        formPanel.add(profileNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        saveFilePathsCheckBox = new JCheckBox("Save last-used file paths with this profile", true);
        formPanel.add(saveFilePathsCheckBox, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Description:"), gbc);
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        descriptionArea = new JTextArea(3, 20);
        formPanel.add(new JScrollPane(descriptionArea), gbc);

        add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            confirmed = true;
            setVisible(false);
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> setVisible(false));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getProfileName() {
        return profileNameField.getText();
    }

    public boolean shouldSaveFilePaths() {
        return saveFilePathsCheckBox.isSelected();
    }

    public String getDescription() {
        return descriptionArea.getText();
    }
}
