package com.excelutility.gui;

import com.excelutility.core.FilteringService;
import com.excelutility.core.expression.FilterExpression;
import com.excelutility.core.expression.GroupNode;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A panel that represents a logical grouping of other filter components.
 * This version supports infix operators (AND/OR between each component).
 */
public class LogicalGroupPanel extends JPanel implements ExpressionNodeComponent {

    private final JTextField groupNameField;
    private final JPanel contentPanel;
    private final JButton addRuleButton;
    private final JButton previewButton;
    private final JLabel recordCountLabel;
    private final ActionListener changeListener;

    /**
     * A panel for the AND/OR radio buttons between components.
     */
    private static class OperatorPanel extends JPanel {
        private final JRadioButton andButton;
        private final JRadioButton orButton;

        public enum OperatorType { RULE, GROUP }

        public OperatorPanel(ActionListener changeListener, OperatorType type) {
            super(new FlowLayout(FlowLayout.CENTER, 5, 0));

            andButton = new JRadioButton("AND");
            orButton = new JRadioButton("OR");

            ActionListener listener = e -> {
                if (changeListener != null) {
                    changeListener.actionPerformed(e);
                }
            };
            andButton.addActionListener(listener);
            orButton.addActionListener(listener);

            ButtonGroup buttonGroup = new ButtonGroup();
            buttonGroup.add(andButton);
            buttonGroup.add(orButton);
            andButton.setSelected(true);

            Color andColor, orColor;
            if (type == OperatorType.RULE) {
                // Rule-level operators: Green for AND, Yellow for OR
                andColor = new Color(0x2F8F6D);
                orColor = new Color(0xF2C94C);
            } else {
                // Group-level operators: Blue for AND, Purple for OR
                andColor = new Color(0x2B7BDE);
                orColor = new Color(0x8E44AD);
            }

            // Set initial color
            setBackground(andColor);
            andButton.setBackground(andColor);
            orButton.setBackground(andColor);

            // Add listeners to change color on selection
            andButton.addActionListener(e -> {
                setBackground(andColor);
                andButton.setBackground(andColor);
                orButton.setBackground(andColor);
            });
            orButton.addActionListener(e -> {
                setBackground(orColor);
                andButton.setBackground(orColor);
                orButton.setBackground(orColor);
            });

            add(andButton);
            add(orButton);
        }

        public FilteringService.LogicalOperator getOperator() {
            return andButton.isSelected() ? FilteringService.LogicalOperator.AND : FilteringService.LogicalOperator.OR;
        }
    }

    public LogicalGroupPanel(String initialName, ActionListener deleteListener, ActionListener changeListener) {
        // Main panel setup
        super(new MigLayout("insets 0, fillx, wrap 1", "[grow]"));
        this.changeListener = changeListener;
        setBorder(BorderFactory.createTitledBorder(initialName));
        // A light blue background for the group header area can be achieved by styling the topBar
        setBackground(Color.WHITE);

        // Top bar for group controls
        JPanel topBar = new JPanel(new MigLayout("insets 2 5 2 5, fillx", "[grow]push[]"));
        topBar.setBackground(new Color(220, 235, 255)); // Light Blue

        groupNameField = new JTextField(initialName);
        groupNameField.setBorder(null);
        groupNameField.setBackground(topBar.getBackground());
        topBar.add(groupNameField, "growx, wmin 100");

        recordCountLabel = new JLabel("(N/A)");
        recordCountLabel.setFont(recordCountLabel.getFont().deriveFont(Font.BOLD));
        topBar.add(recordCountLabel, "gapleft 10");

        previewButton = new JButton("Preview");
        topBar.add(previewButton, "gapleft 10");

        addRuleButton = new JButton("Add Rule");
        topBar.add(addRuleButton);

        if (deleteListener != null) {
            JButton deleteGroupButton = new JButton("X");
            deleteGroupButton.setToolTipText("Delete this group");
            deleteGroupButton.setMargin(new Insets(1, 1, 1, 1));
            deleteGroupButton.addActionListener(e -> deleteListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null)));
            topBar.add(deleteGroupButton);
        }

        add(topBar, "growx");

        // Content panel for rules and subgroups
        contentPanel = new JPanel(new MigLayout("insets 5 10 5 10, fillx, wrap 1", "[grow]")); // Indented content
        add(contentPanel, "growx");
    }

    @Override
    public void setBorder(javax.swing.border.Border border) {
        // To handle the groupNameField also being part of the border
        if (groupNameField != null && border instanceof javax.swing.border.TitledBorder) {
            groupNameField.setText(((javax.swing.border.TitledBorder) border).getTitle());
        }
        super.setBorder(border);
    }

    public void addComponent(Component component) {
        if (!(component instanceof ExpressionNodeComponent)) {
            throw new IllegalArgumentException("Only ExpressionNodeComponents can be added to a LogicalGroupPanel.");
        }
        // Add an operator panel if this is not the first component
        if (contentPanel.getComponentCount() > 0) {
            OperatorPanel.OperatorType type = (component instanceof FilterRulePanel) ? OperatorPanel.OperatorType.RULE : OperatorPanel.OperatorType.GROUP;
            contentPanel.add(new OperatorPanel(this.changeListener, type), "growx, align center");
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
            return new GroupNode(FilteringService.LogicalOperator.AND, groupNameField.getText()); // Return an empty, valid group
        }
        if (expressions.size() == 1) {
            // If there is only one expression, we don't need to wrap it in a group node for evaluation,
            // but we might lose the group's name. For filtering, this is fine.
            return expressions.get(0);
        }

        // Build the expression tree from left to right
        FilterExpression leftOperand = expressions.get(0);
        for (int i = 0; i < operators.size(); i++) {
            FilteringService.LogicalOperator op = operators.get(i);
            FilterExpression rightOperand = expressions.get(i + 1);
            // The name "Sub-expression" is temporary for unnamed intermediate nodes
            GroupNode newNode = new GroupNode(op, "Sub-expression");
            newNode.addChild(leftOperand);
            newNode.addChild(rightOperand);
            leftOperand = newNode;
        }

        // Wrap the final result in a named group node so the group's name is preserved
        GroupNode rootGroup = new GroupNode(FilteringService.LogicalOperator.AND, groupNameField.getText());
        rootGroup.addChild(leftOperand);
        return rootGroup;
    }

    public JButton getAddRuleButton() {
        return addRuleButton;
    }

    public JButton getPreviewButton() {
        return previewButton;
    }

    public String getName() {
        return groupNameField.getText();
    }

    public List<LogicalGroupPanel> getAllGroupPanels() {
        List<LogicalGroupPanel> panels = new java.util.ArrayList<>();
        panels.add(this);
        for (Component comp : contentPanel.getComponents()) {
            if (comp instanceof LogicalGroupPanel) {
                panels.addAll(((LogicalGroupPanel) comp).getAllGroupPanels());
            }
        }
        return panels;
    }

    public void setRecordCount(int count) {
        if (count < 0) {
            recordCountLabel.setText("(Error)");
            recordCountLabel.setForeground(Color.ORANGE);
        } else {
            recordCountLabel.setText("(" + count + ")");
            if (count == 0) {
                recordCountLabel.setForeground(Color.RED);
            } else {
                recordCountLabel.setForeground(new Color(0, 153, 0)); // Dark Green
            }
        }
    }
}
