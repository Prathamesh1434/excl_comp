package com.excelutility.gui;

import com.excelutility.core.FilteringService;
import com.excelutility.core.GroupState;
import com.excelutility.core.RuleState;
import com.excelutility.core.expression.FilterExpression;
import com.excelutility.core.expression.GroupNode;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LogicalGroupPanel extends JPanel implements ExpressionNodeComponent {

    private final JTextField groupNameField;
    private final JPanel contentPanel;
    private final JButton addRuleButton;
    private final JLabel recordCountLabel;
    private final JToggleButton interGroupConnectorToggle;

    public LogicalGroupPanel(String initialName, ActionListener deleteListener) {
        super(new MigLayout("insets 0, fillx, wrap 1", "[grow]"));
        setBorder(BorderFactory.createTitledBorder(initialName));
        setBackground(Color.WHITE);

        JPanel topBar = new JPanel(new MigLayout("insets 2 5 2 5, fillx", "[grow]push[]"));
        topBar.setBackground(new Color(220, 235, 255));

        groupNameField = new JTextField(initialName);
        groupNameField.setBorder(null);
        groupNameField.setBackground(topBar.getBackground());
        topBar.add(groupNameField, "growx, wmin 100");

        recordCountLabel = new JLabel("(N/A)");
        topBar.add(recordCountLabel, "gapleft 10");

        addRuleButton = new JButton("Add Rule");
        topBar.add(addRuleButton);

        interGroupConnectorToggle = new JToggleButton("AND");
        configureConnector(interGroupConnectorToggle, "purple");
        topBar.add(interGroupConnectorToggle);

        if (deleteListener != null) {
            JButton deleteGroupButton = new JButton("X");
            deleteGroupButton.addActionListener(deleteListener);
            topBar.add(deleteGroupButton);
        }

        add(topBar, "growx");
        contentPanel = new JPanel(new MigLayout("insets 5 10 5 10, fillx, wrap 1", "[grow]"));
        add(contentPanel, "growx");
    }

    private void configureConnector(JToggleButton toggleButton, String color) {
        toggleButton.setBackground(color.equalsIgnoreCase("purple") ? Color.MAGENTA : Color.ORANGE);
        toggleButton.addItemListener(e -> {
            JToggleButton source = (JToggleButton) e.getSource();
            source.setText(source.isSelected() ? "OR" : "AND");
        });
    }

    public void addComponent(Component component) {
        contentPanel.add(component, "growx");
        revalidateAndRepaint();
    }

    public void removeComponent(Component component) {
        contentPanel.remove(component);
        revalidateAndRepaint();
    }

    private void revalidateAndRepaint() {
        contentPanel.revalidate();
        contentPanel.repaint();
        if (getParent() != null) {
            getParent().revalidate();
            getParent().repaint();
        }
    }

    @Override
    public void removeAll() {
        contentPanel.removeAll();
        revalidateAndRepaint();
    }

    public GroupState getGroupState() {
        List<RuleState> ruleStates = new ArrayList<>();
        for (Component comp : contentPanel.getComponents()) {
            if (comp instanceof FilterRulePanel) {
                ruleStates.add(((FilterRulePanel) comp).getRuleState());
            }
        }

        long count = 0;
        try {
            count = Long.parseLong(recordCountLabel.getText().replaceAll("[^\\d-]", ""));
        } catch (NumberFormatException e) { /* ignore */ }

        return new GroupState(
            groupNameField.getText(),
            getIntraGroupConnector(), // This needs to be implemented properly
            ruleStates,
            interGroupConnectorToggle.isSelected() ? FilteringService.LogicalOperator.OR : FilteringService.LogicalOperator.AND,
            "purple", // hardcoded for now
            count
        );
    }

    // This is a simplification. A real implementation would get this from the UI.
    public FilteringService.LogicalOperator getIntraGroupConnector() {
        return FilteringService.LogicalOperator.AND;
    }

    public void setIntraGroupConnector(FilteringService.LogicalOperator op) {
        // In a real implementation, this would set the state of the intra-group connector UI
    }

    public void setInterGroupConnector(FilteringService.LogicalOperator op) {
        interGroupConnectorToggle.setSelected(op == FilteringService.LogicalOperator.OR);
    }

    public void setConnectorColor(String color) {
        // In a real implementation, this would set the color of the connector UI
    }

    @Override
    public FilterExpression getExpression() {
        List<FilterExpression> expressions = Arrays.stream(contentPanel.getComponents())
                .filter(c -> c instanceof ExpressionNodeComponent)
                .map(c -> ((ExpressionNodeComponent) c).getExpression())
                .collect(Collectors.toList());

        GroupNode node = new GroupNode(getIntraGroupConnector(), groupNameField.getText());
        expressions.forEach(node::addChild);
        return node;
    }

    public JButton getAddRuleButton() {
        return addRuleButton;
    }

    public void setRecordCount(long count) {
        recordCountLabel.setText("(" + count + ")");
    }
}
