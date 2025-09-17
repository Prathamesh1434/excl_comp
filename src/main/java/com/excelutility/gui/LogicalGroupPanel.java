package com.excelutility.gui;

import com.excelutility.core.FilteringService;
import com.excelutility.core.expression.FilterExpression;
import com.excelutility.core.expression.GroupNode;
import com.excelutility.core.expression.RuleNode;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A panel that represents a logical grouping of other filter components.
 * This version supports infix operators (AND/OR between each component).
 */
public class LogicalGroupPanel extends JPanel implements ExpressionNodeComponent {

    private final JTextField groupNameField;
    private final JLabel recordCountLabel;
    private final JPanel contentPanel;
    private final JButton addRuleButton;
    private final FilterPanel panelProvider;

    /**
     * A panel for the AND/OR radio buttons between components.
     */
    private static class OperatorPanel extends JPanel {
        private final JRadioButton andButton;
        private final JRadioButton orButton;

        public OperatorPanel() {
            super(new FlowLayout(FlowLayout.CENTER, 5, 0));
            setBackground(new Color(255, 255, 224)); // Pale Yellow
            andButton = new JRadioButton("AND");
            andButton.setBackground(getBackground());
            orButton = new JRadioButton("OR");
            orButton.setBackground(getBackground());

            ButtonGroup group = new ButtonGroup();
            group.add(andButton);
            group.add(orButton);
            andButton.setSelected(true); // Default to AND
            add(andButton);
            add(orButton);
        }

        public FilteringService.LogicalOperator getOperator() {
            return andButton.isSelected() ? FilteringService.LogicalOperator.AND : FilteringService.LogicalOperator.OR;
        }
    }

    public LogicalGroupPanel(String initialName, FilterPanel panelProvider, ActionListener deleteListener) {
        // Main panel setup
        super(new MigLayout("insets 5, fillx, wrap 1", "[grow]"));
        this.panelProvider = panelProvider;
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
        setBackground(new Color(220, 235, 255)); // Light Blue

        // Top bar for group controls
        JPanel topBar = new JPanel(new MigLayout("insets 2", "[grow]rel[auto]rel[auto]push[auto]rel[auto]"));
        topBar.setBackground(getBackground());

        groupNameField = new JTextField(initialName);
        topBar.add(groupNameField, "growx, wmin 150");

        recordCountLabel = new JLabel("(N/A)");
        recordCountLabel.setFont(recordCountLabel.getFont().deriveFont(Font.BOLD));
        topBar.add(recordCountLabel);

        JButton previewButton = new JButton("Preview");
        previewButton.setToolTipText("Preview results for this entire group in a new tab");
        previewButton.addActionListener(e -> panelProvider.previewGroup(this));
        topBar.add(previewButton);

        addRuleButton = new JButton("Add Rule");
        topBar.add(addRuleButton, "gapleft 20");

        if (deleteListener != null) {
            JButton deleteGroupButton = new JButton("Delete Group");
            deleteGroupButton.addActionListener(e -> deleteListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null)));
            topBar.add(deleteGroupButton);
        }

        add(topBar, "growx");

        // Content panel for rules and subgroups
        contentPanel = new JPanel(new MigLayout("insets 5 0 0 0, fillx, wrap 1", "[grow]"));
        contentPanel.setOpaque(false);
        add(contentPanel, "growx, gaptop 5");
    }

    public void addComponent(Component component) {
        if (!(component instanceof ExpressionNodeComponent)) {
            throw new IllegalArgumentException("Only ExpressionNodeComponents can be added to a LogicalGroupPanel.");
        }
        // Add an operator panel if this is not the first component
        if (contentPanel.getComponentCount() > 0) {
            contentPanel.add(new OperatorPanel(), "growx, align center");
        }
        contentPanel.add(component, "growx");
        revalidateAndRepaint();
    }

    public void removeComponent(Component component) {
        List<Component> components = Arrays.asList(contentPanel.getComponents());
        int index = components.indexOf(component);

        if (index != -1) {
            // If we're removing the first element, also remove the operator after it (if it exists)
            if (index == 0 && components.size() > 1) {
                contentPanel.remove(components.get(1));
            }
            // If we're removing any other element, remove the operator before it
            else if (index > 0) {
                contentPanel.remove(components.get(index - 1));
            }
            contentPanel.remove(component);
            revalidateAndRepaint();
        }
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

    @Override
    public FilterExpression getExpression() {
        List<Component> components = Arrays.asList(contentPanel.getComponents());
        List<FilterExpression> expressions = components.stream()
                .filter(c -> c instanceof ExpressionNodeComponent)
                .map(c -> ((ExpressionNodeComponent) c).getExpression())
                .collect(Collectors.toList());

        List<FilteringService.LogicalOperator> operators = components.stream()
                .filter(c -> c instanceof OperatorPanel)
                .map(c -> ((OperatorPanel) c).getOperator())
                .collect(Collectors.toList());

        if (expressions.isEmpty()) {
            return new GroupNode(FilteringService.LogicalOperator.AND, getGroupName()); // Return an empty, valid group
        }
        if (expressions.size() == 1) {
            return expressions.get(0);
        }

        // Build the expression tree from left to right
        FilterExpression leftOperand = expressions.get(0);
        for (int i = 0; i < operators.size(); i++) {
            FilteringService.LogicalOperator op = operators.get(i);
            FilterExpression rightOperand = expressions.get(i + 1);
            GroupNode newNode = new GroupNode(op, "Sub-expression");
            newNode.addChild(leftOperand);
            newNode.addChild(rightOperand);
            leftOperand = newNode;
        }

        // Wrap the final result in a named group node
        GroupNode rootGroup = new GroupNode(FilteringService.LogicalOperator.AND, getGroupName());
        rootGroup.addChild(leftOperand);
        return rootGroup;
    }

    public String getGroupName() {
        return groupNameField.getText();
    }

    public void setGroupName(String name) {
        groupNameField.setText(name);
    }

    public void setRecordCount(int count) {
        recordCountLabel.setText("(" + count + ")");
        if (count == 0) {
            recordCountLabel.setForeground(Color.RED);
        } else {
            recordCountLabel.setForeground(new Color(0, 153, 0)); // Dark Green
        }
    }

    public JButton getAddRuleButton() {
        return addRuleButton;
    }

    // setOperator and getAddGroupButton are no longer needed.
}
