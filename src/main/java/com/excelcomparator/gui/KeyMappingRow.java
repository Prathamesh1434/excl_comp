package com.excelcomparator.gui;

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * Represents a single row in the key mapping UI, containing dropdowns for column selection
 * from each file and a remove button.
 */
public class KeyMappingRow extends JPanel {
    private final JComboBox<String> file1ColumnComboBox;
    private final JComboBox<String> file2ColumnComboBox;
    private final JButton removeButton;

    public KeyMappingRow() {
        file1ColumnComboBox = new JComboBox<>();
        file2ColumnComboBox = new JComboBox<>();
        removeButton = new JButton("-");

        add(new JLabel("File 1 Column:"));
        add(file1ColumnComboBox);
        add(new JLabel("vs File 2 Column:"));
        add(file2ColumnComboBox);
        add(removeButton);
    }

    /**
     * Sets the action listener for the remove button.
     *
     * @param listener The ActionListener to add.
     */
    public void setRemoveButtonAction(ActionListener listener) {
        // Remove old listeners to prevent duplicates
        for (ActionListener al : removeButton.getActionListeners()) {
            removeButton.removeActionListener(al);
        }
        removeButton.addActionListener(listener);
    }

    /**
     * Gets the JComboBox for File 1's columns.
     *
     * @return The JComboBox for File 1.
     */
    public JComboBox<String> getFile1ColumnComboBox() {
        return file1ColumnComboBox;
    }

    /**
     * Gets the JComboBox for File 2's columns.
     *
     * @return The JComboBox for File 2.
     */
    public JComboBox<String> getFile2ColumnComboBox() {
        return file2ColumnComboBox;
    }
}
