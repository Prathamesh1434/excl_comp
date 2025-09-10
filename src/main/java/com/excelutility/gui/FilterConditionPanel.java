package com.excelutility.gui;

import com.excelutility.core.Operator;
import javax.swing.*;
import java.util.List;

/**
 * A panel representing a single filter condition row in the Filter Builder UI.
 */
public class FilterConditionPanel extends JPanel {

    private final JComboBox<String> columnCombo;
    private final JComboBox<Operator> operatorCombo;
    private final JTextField valueField;
    private final JButton removeButton;

    public FilterConditionPanel(List<String> availableColumns) {
        columnCombo = new JComboBox<>(availableColumns.toArray(new String[0]));
        operatorCombo = new JComboBox<>(Operator.values());
        valueField = new JTextField(15);
        removeButton = new JButton("-");

        add(new JLabel("Column:"));
        add(columnCombo);
        add(new JLabel("Operator:"));
        add(operatorCombo);
        add(new JLabel("Value:"));
        add(valueField);
        add(removeButton);
    }

    public JButton getRemoveButton() {
        return removeButton;
    }

    // Add getters for the other components to retrieve the user's selections
}
