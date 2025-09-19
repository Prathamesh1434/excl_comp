package com.excelutility.gui;

import com.excelutility.core.FilterRule;
import com.excelutility.core.Operator;
import com.excelutility.core.RuleState;
import com.excelutility.core.expression.FilterExpression;
import com.excelutility.core.expression.RuleNode;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

public class FilterRulePanel extends JPanel implements ExpressionNodeComponent {

    private final JTextField ruleNameField;
    private final JComboBox<String> columnComboBox;
    private final JComboBox<Operator> operatorComboBox;
    private final JTextField valueField;
    private final JLabel recordCountLabel;
    private final JToggleButton connectorToggle;

    public FilterRulePanel(RuleState initialState, ActionListener deleteListener, List<String> availableColumns) {
        setLayout(new MigLayout("insets 2 5 2 5, fillx", "[grow]rel[]rel[]rel[]rel[]rel[]"));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(224, 224, 224)));
        setBackground(Color.WHITE);

        ruleNameField = new JTextField(initialState.getName());
        columnComboBox = new JComboBox<>(availableColumns.toArray(new String[0]));
        operatorComboBox = new JComboBox<>(Operator.values());
        valueField = new JTextField(initialState.getValue());
        recordCountLabel = new JLabel("(N/A)");
        connectorToggle = new JToggleButton("AND");

        ruleNameField.setBorder(null);
        columnComboBox.setSelectedItem(initialState.getColumnName());
        operatorComboBox.setSelectedItem(initialState.getOperator());
        setRecordCount(initialState.getRecordCount());
        configureConnector(connectorToggle, initialState.getConnectorColor());
        connectorToggle.setSelected(initialState.getConnector() == com.excelutility.core.FilteringService.LogicalOperator.OR);


        add(ruleNameField, "wmin 80");
        add(columnComboBox, "wmin 150");
        add(operatorComboBox, "wmin 100");
        add(valueField, "growx, wmin 100");
        add(recordCountLabel, "gapleft 10");
        add(connectorToggle, "gapleft 10");

        JButton deleteButton = new JButton("X");
        deleteButton.addActionListener(deleteListener);
        add(deleteButton);
    }

    private void configureConnector(JToggleButton toggleButton, String color) {
        // Simplified color logic
        toggleButton.setBackground(color.equalsIgnoreCase("yellow") ? Color.YELLOW : Color.CYAN);
        toggleButton.addItemListener(e -> {
            JToggleButton source = (JToggleButton) e.getSource();
            source.setText(source.isSelected() ? "OR" : "AND");
        });
    }

    public RuleState getRuleState() {
        return new RuleState(
                ruleNameField.getText(),
                (String) columnComboBox.getSelectedItem(),
                (Operator) operatorComboBox.getSelectedItem(),
                valueField.getText(),
                connectorToggle.isSelected() ? com.excelutility.core.FilteringService.LogicalOperator.OR : com.excelutility.core.FilteringService.LogicalOperator.AND,
                "yellow", // Hardcoded for now
                Long.parseLong(recordCountLabel.getText().replaceAll("[^\\d-]", ""))
        );
    }

    public void setRecordCount(long count) {
        recordCountLabel.setText("(" + count + ")");
        if (count == 0) recordCountLabel.setForeground(Color.RED);
        else recordCountLabel.setForeground(Color.GREEN.darker());
    }

    @Override
    public FilterExpression getExpression() {
        FilterRule rule = new FilterRule(
                (String) columnComboBox.getSelectedItem(),
                (Operator) operatorComboBox.getSelectedItem(),
                valueField.getText()
        );
        RuleNode node = new RuleNode(rule);
        node.setName(ruleNameField.getText());
        return node;
    }
}
